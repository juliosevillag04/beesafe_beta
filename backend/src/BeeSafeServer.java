import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/** BeeSafe beta: servidor Java 17+ sin bibliotecas externas. Escenario educativo ficticio. */
public class BeeSafeServer {
  static final Object LOCK = new Object();
  static final String HOST=System.getenv().getOrDefault("BEESAFE_HOST", "127.0.0.1");
  static final int PORT=Integer.parseInt(System.getenv().getOrDefault("PORT", System.getenv().getOrDefault("BEESAFE_PORT", "8765")));
  static final Path ROOT=Paths.get("").toAbsolutePath().normalize();
  static final Path WEB=ROOT.resolve("web").normalize();
  static final Path DATA=ROOT.resolve("datos/estado.json");
  static final CopyOnWriteArrayList<OutputStream> subscribers=new CopyOnWriteArrayList<>();
  static final ScheduledExecutorService scheduler=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"beesafe-simulator");t.setDaemon(true);return t;});
  static Map<String,Object> state;
  static long version=0;
  static long lastSave=0;
  static final AtomicLong ids=new AtomicLong(1000);
  static volatile boolean running=true;
  public static void main(String[] args) throws Exception {
    Files.createDirectories(DATA.getParent());
    synchronized(LOCK){ load(); setSimStoppedOnBoot(); save(); }
    HttpServer server=HttpServer.create(new InetSocketAddress(InetAddress.getByName(HOST),PORT),0);
    server.createContext("/", BeeSafeServer::handle);
    server.setExecutor(Executors.newCachedThreadPool());
    Runtime.getRuntime().addShutdownHook(new Thread(()->{synchronized(LOCK){save();} running=false;server.stop(1);scheduler.shutdownNow();}));
    scheduler.scheduleAtFixedRate(()->{try{tick();}catch(Exception e){System.err.println("Sim error: "+e.getMessage());}},1,1,TimeUnit.SECONDS);
    server.start();
    System.out.println("BeeSafe beta lista en http://"+HOST+":"+PORT);
  }
  @SuppressWarnings("unchecked") static Map<String,Object> map(Object value){return value instanceof Map<?,?> ? (Map<String,Object>)value:new LinkedHashMap<>();}
  @SuppressWarnings("unchecked") static List<Object> arr(Object value){return value instanceof List<?> ? (List<Object>)value:new ArrayList<>();}
  static String str(Object x){return x==null?"":String.valueOf(x);}
  static double num(Object x,double fallback){try{return x instanceof Number ? ((Number)x).doubleValue():Double.parseDouble(str(x));}catch(Exception e){return fallback;}}
  static int integer(Object x,int fallback){return (int)Math.round(num(x,fallback));}
  static String now(){return Instant.now().toString();}
  static Map<String,Object> obj(Object... pairs){Map<String,Object> m=new LinkedHashMap<>();for(int i=0;i<pairs.length;i+=2)m.put(str(pairs[i]),pairs[i+1]);return m;}
  static String unique(String prefix){return prefix+"-"+ids.incrementAndGet();}
  static void load(){
    try{if(Files.exists(DATA)){Object parsed=Json.read(Files.readString(DATA));state=map(parsed);if(!state.containsKey("schemaVersion"))throw new IllegalArgumentException("schemaVersion faltante");return;}}
    catch(Exception ex){System.err.println("Estado anterior ilegible; copia de seguridad: "+ex);try{Files.copy(DATA,DATA.resolveSibling("estado-corrupto-"+System.currentTimeMillis()+".json"));}catch(Exception ignored){}}
    state=seed();
  }
  static void setSimStoppedOnBoot(){Map<String,Object> sim=map(state.get("simulation"));if(str(sim.get("status")).equals("running")||str(sim.get("status")).equals("paused")){sim.put("status","stopped");sim.put("message","La sesión se detuvo al cerrar el servidor. Se conservó el historial.");}}
  static Map<String,Object> seed(){
    Map<String,Object> m=obj("schemaVersion",1,"apiary",obj("name","Apiario Los Girasoles","region","Masaya, Nicaragua","description","Apiario ficticio de demostración para el proyecto APS de Física II. No representa una granja ni mediciones reales.","species","Apis mellifera"),
      "hives",new ArrayList<>(Arrays.asList(
        obj("id","COL-001","name","Colmena Norte","sector","Sector norte","species","Apis mellifera","active",true,"notes","Colmena inicial del escenario"),
        obj("id","COL-002","name","Colmena Central","sector","Sector central","species","Apis mellifera","active",true,"notes","Punto principal para observar la variación simulada"),
        obj("id","COL-003","name","Colmena Sur","sector","Sector sur","species","Apis mellifera","active",true,"notes","Colmena de comparación"))),
      "sources",new ArrayList<>(Arrays.asList(
        obj("id","FUE-001","name","Línea eléctrica cercana","type","Línea de distribución","sector","Límite norte","frequencyHz",60,"notes","Fuente ficticia de baja frecuencia. Modelo descriptivo, no cálculo de exposición real.","active",true),
        obj("id","FUE-002","name","Transformador de distribución","type","Transformador","sector","Acceso este","frequencyHz",60,"notes","Referencia ficticia del escenario educativo.","active",true))),
      "measurements",new ArrayList<>(),"observations",new ArrayList<>(),"notes",new ArrayList<>(),"tasks",new ArrayList<>(),"events",new ArrayList<>(),
      "simulation",obj("status","idle","seconds",0,"startedAt",null,"runId",null,"message","Lista para iniciar","seed",2026,"lastTick",null));
    List<Object> observations=arr(m.get("observations"));
    int[][] data={{94,102},{88,92},{99,105},{91,89},{96,103},{87,90}};
    for(int i=0;i<data.length;i++){
      String hive="COL-00"+(i%3+1);
      observations.add(obj("id","OBS-DEMO-"+(i+1),"hiveId",hive,"entered",data[i][0],"exited",data[i][1],"minutes",10,
        "temperature",29+(i%3),"humidity",65+(i%4)*2,"weather","Soleado","flowers","Media","notes","Observación histórica ficticia, independiente de las mediciones electromagnéticas.","origin","demo-historica","actor","Sistema demo","at",Instant.now().minusSeconds((7-i)*3600L).toString()));
    }
    arr(m.get("tasks")).add(obj("id","SEG-DEMO-1","hiveId","COL-002","title","Revisión periódica del entorno","details","Verificar que las mediciones estén documentadas y registrar observaciones de actividad.","status","pendiente","actor","Técnico","at",now()));
    arr(m.get("events")).add(obj("id","EV-DEMO","type","info","message","Apiario de demostración creado con tres colmenas y dos fuentes ficticias.","at",now()));
    return m;
  }
  static void save(){try{String value=Json.write(state);Path tmp=DATA.resolveSibling("estado.tmp");Files.writeString(tmp,value,StandardCharsets.UTF_8);try{Files.move(tmp,DATA,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(Exception e){Files.move(tmp,DATA,StandardCopyOption.REPLACE_EXISTING);}lastSave=System.currentTimeMillis();}catch(Exception e){System.err.println("Error guardando: "+e.getMessage());}}
  static void changed(){version++;if(System.currentTimeMillis()-lastSave>5000)save();}
  static void event(String type,String message){List<Object> events=arr(state.get("events"));events.add(obj("id",unique("EV"),"type",type,"message",message,"at",now()));if(events.size()>150)events.subList(0,events.size()-150).clear();}
  static String status(){return str(map(state.get("simulation")).get("status"));}
  static void tick(){synchronized(LOCK){
    if(!status().equals("running"))return;
    Map<String,Object> sim=map(state.get("simulation"));int t=integer(sim.get("seconds"),0)+1;sim.put("seconds",t);sim.put("lastTick",now());
    List<Object> hives=arr(state.get("hives")), measurements=arr(state.get("measurements"));int index=0;
    for(Object ho:hives){Map<String,Object> hive=map(ho);if(!Boolean.TRUE.equals(hive.get("active")))continue;
      String id=str(hive.get("id"));double baseB= id.equals("COL-001")?.80:id.equals("COL-002")?.35:id.equals("COL-003")?.15:.22+(index*.07);
      double baseE= id.equals("COL-001")?18:id.equals("COL-002")?12:id.equals("COL-003")?7:9+(index*2);
      double driftB=.018*Math.sin(t*.19+index*1.3)+.008*Math.cos(t*.57+index);
      double driftE=.55*Math.sin(t*.15+index*1.9)+.3*Math.cos(t*.39+index);
      // Una sola secuencia lineal reproducible: cambio transitorio no causal en COL-002.
      double rise=(t>=48&&t<=110&&id.equals("COL-002"))? .27*Math.min(1,Math.min((t-48)/14.0,(110-t)/16.0)):0;
      double b=Math.max(.001,baseB+driftB+rise);
      double e=Math.max(.1,baseE+driftE+(rise>0?6*rise/.27:0));
      measurements.add(measurement(id,"B",round(b,4),"µT",60,"simulada","Simulador BeeSafe (lectura RMS ficticia)",sim.get("runId"),""));
      measurements.add(measurement(id,"E",round(e,3),"V/m",60,"simulada","Simulador BeeSafe (lectura RMS ficticia)",sim.get("runId"),""));index++;
    }
    if(t==48)event("revision","Se inicia una variación programada de la fuente ficticia cerca de COL-002; no implica riesgo biológico.");
    if(t==85)event("info","La intensidad simulada continúa cambiando. Consultar la tendencia de COL-002.");
    if(t==111)event("info","Finalizó la variación programada. El monitoreo continúa.");
    // Mantener historial acotado sin afectar datos ingresados manualmente.
    if(measurements.size()>12000){int n=measurements.size()-10500;for(int i=0;i<n;i++){boolean removed=false;for(int j=0;j<measurements.size();j++){if(str(map(measurements.get(j)).get("origin")).equals("simulada")){measurements.remove(j);removed=true;break;}}if(!removed)break;}}
    changed();broadcast("tick",obj("version",version,"simulation",sim,"latest",latestForAll(),"measurementCount",measurements.size()));
  }}
  static double round(double d,int n){double k=Math.pow(10,n);return Math.round(d*k)/k;}
  static Map<String,Object> measurement(String hiveId,String field,double value,String unit,int hz,String origin,String instrument,Object runId,String note){return obj("id",unique("MED"),"hiveId",hiveId,"field",field,"value",value,"unit",unit,"frequencyHz",hz,"origin",origin,"instrument",instrument,"runId",runId,"at",now(),"notes",note);}
  static Map<String,Object> latestForAll(){Map<String,Object> out=new LinkedHashMap<>();List<Object> mm=arr(state.get("measurements"));for(int i=mm.size()-1;i>=0;i--){Map<String,Object> m=map(mm.get(i));String key=str(m.get("hiveId"))+"_"+str(m.get("field"));out.putIfAbsent(key,m);if(out.size()>=arr(state.get("hives")).size()*2)break;}return out;}
  static Map<String,Object> live(){
    Map<String,Object> sim=map(state.get("simulation"));
    List<Object> mm=arr(state.get("measurements")); List<Object> recent=new ArrayList<>();
    for(int i=Math.max(0,mm.size()-540);i<mm.size();i++)recent.add(mm.get(i));
    return obj("simulation",sim,"latest",latestForAll(),"recent",recent,"events",state.get("events"),"total",mm.size(),"version",version);
  }
  static void broadcast(String event,Object obj){byte[] bytes=("event: "+event+"\ndata: "+Json.write(obj)+"\n\n").getBytes(StandardCharsets.UTF_8);for(OutputStream os:subscribers){try{os.write(bytes);os.flush();}catch(Exception e){subscribers.remove(os);try{os.close();}catch(Exception ignored){}}}}
  static void respond(HttpExchange x,int code,Object obj) throws IOException{byte[] bytes=Json.write(obj).getBytes(StandardCharsets.UTF_8);x.getResponseHeaders().set("Content-Type","application/json; charset=utf-8");x.getResponseHeaders().set("Cache-Control","no-store");x.sendResponseHeaders(code,bytes.length);try(OutputStream os=x.getResponseBody()){os.write(bytes);}}
  static void fail(HttpExchange x,int code,String error) throws IOException{respond(x,code,obj("ok",false,"error",error));}
  static Map<String,Object> body(HttpExchange x) throws IOException{byte[] bytes=x.getRequestBody().readNBytes(1024*1024+1);if(bytes.length>1024*1024)throw new IllegalArgumentException("Datos excesivos");return map(Json.read(new String(bytes,StandardCharsets.UTF_8)));}
  static String field(Map<String,Object> m,String name,int max){String s=str(m.get(name)).trim();if(s.length()>max)throw new IllegalArgumentException(name+" supera "+max+" caracteres");return s;}
  static String need(Map<String,Object> m,String name,int max){String s=field(m,name,max);if(s.isBlank())throw new IllegalArgumentException("Campo obligatorio: "+name);return s;}
  static boolean validHive(String id){for(Object h:arr(state.get("hives")))if(str(map(h).get("id")).equals(id))return true;return false;}
  static void checkHive(String id){if(!validHive(id))throw new IllegalArgumentException("La colmena no existe");}
  static String role(HttpExchange x){return x.getRequestHeaders().getFirst("X-Demo-Role")==null?"guest":x.getRequestHeaders().getFirst("X-Demo-Role");}
  static boolean is(String role,String... allowed){return Arrays.asList(allowed).contains(role);}
  static void handle(HttpExchange x) throws IOException{
    String p=x.getRequestURI().getPath(), method=x.getRequestMethod();
    try{
      if(p.equals("/api/shutdown")&&method.equals("POST")){
        if(!"127.0.0.1".equals(HOST)){fail(x,403,"Cierre remoto deshabilitado.");return;}
        if(!role(x).equals("tecnico")){fail(x,403,"Solo el perfil técnico puede solicitar el cierre local.");return;}
        synchronized(LOCK){save();}
        respond(x,200,obj("ok",true,"message","Cierre solicitado; datos conservados."));
        new Thread(()->{try{Thread.sleep(300);}catch(InterruptedException ignored){}System.exit(0);},"beesafe-close").start();
        return;
      }
      if(p.equals("/api/events")&&method.equals("GET")){stream(x);return;}
      if(p.equals("/api/health")){respond(x,200,obj("ok",true,"app","BeeSafe beta","version",1,"runtime","Java 17+"));return;}
      if(p.equals("/api/state")&&method.equals("GET")){synchronized(LOCK){respond(x,200,obj("ok",true,"data",state,"version",version));}return;}
      if(p.equals("/api/live")&&method.equals("GET")){synchronized(LOCK){respond(x,200,obj("ok",true,"data",live()));}return;}
      if(p.equals("/api/export.csv")&&method.equals("GET")){exportCsv(x);return;}
      if(p.startsWith("/api/")&&!method.equals("POST")){fail(x,405,"Método no permitido");return;}
      if(p.startsWith("/api/")&&method.equals("POST")){
        Map<String,Object> input=body(x);String actor=role(x);if(!is(actor,"apicultor","tecnico","investigador")){fail(x,403,"Seleccione un rol de demostración válido.");return;}
        synchronized(LOCK){
          Object output=mutate(p,input,actor);
          changed();save();broadcast("update",obj("version",version,"reason",p));respond(x,200,obj("ok",true,"data",output,"version",version));
        }return;
      }
      if(!method.equals("GET")){fail(x,405,"Método no permitido");return;}
      staticFile(x,p);
    }catch(IllegalArgumentException e){fail(x,400,e.getMessage());}catch(SecurityException e){fail(x,403,e.getMessage());}catch(Exception e){System.err.println("Error "+p+": "+e);fail(x,500,"Error interno: "+e.getMessage());}
  }
  static void authorized(String actor,String... roles){if(!is(actor,roles))throw new SecurityException("El rol "+actor+" no tiene permiso para esta operación.");}
  static Object mutate(String p,Map<String,Object> input,String actor){
    Map<String,Object> sim=map(state.get("simulation"));String action=field(input,"action",30);
    if(p.equals("/api/simulation")){
      authorized(actor,"tecnico");
      if(action.equals("start")){
        if(status().equals("running")||status().equals("paused"))throw new IllegalArgumentException("Ya existe una simulación iniciada.");
        sim.put("status","running");sim.put("seconds",0);sim.put("startedAt",now());sim.put("runId",unique("RUN"));sim.put("lastTick",null);sim.put("message","Simulación compartida en ejecución");event("success","Simulación única iniciada por el técnico. Todos los datos generados son ficticios.");
      }else if(action.equals("pause")){if(!status().equals("running"))throw new IllegalArgumentException("La simulación no está activa.");sim.put("status","paused");sim.put("message","Generación en pausa");event("info","Simulación pausada.");}
      else if(action.equals("resume")){if(!status().equals("paused"))throw new IllegalArgumentException("La simulación no está pausada.");sim.put("status","running");sim.put("message","Simulación compartida en ejecución");event("success","Simulación reanudada.");}
      else if(action.equals("stop")){if(!is(status(),"running","paused"))throw new IllegalArgumentException("No hay simulación que finalizar.");sim.put("status","stopped");sim.put("message","Sesión finalizada. Historial conservado.");event("info","El técnico finalizó la simulación.");}
      else if(action.equals("reset")){state=seed();event("info","Entorno de demostración restablecido por el técnico.");}
      else throw new IllegalArgumentException("Acción de simulación desconocida.");
      return state.get("simulation");
    }
    if(p.equals("/api/hives")){
      authorized(actor,"tecnico");String name=need(input,"name",80),sector=need(input,"sector",100);
      String id="COL-"+String.format("%03d",arr(state.get("hives")).size()+1);
      Object o=obj("id",id,"name",name,"sector",sector,"species",field(input,"species",80).isBlank()?"Apis mellifera":field(input,"species",80),"active",true,"notes",field(input,"notes",500));
      arr(state.get("hives")).add(o);event("success","Nueva colmena "+id+" registrada. Aún no tiene historial de medición.");return o;
    }
    if(p.equals("/api/sources")){
      authorized(actor,"tecnico");String name=need(input,"name",100), type=need(input,"type",100);double hz=num(input.get("frequencyHz"),60);
      if(hz<0||hz>1e10)throw new IllegalArgumentException("Frecuencia inválida.");
      Object o=obj("id","FUE-"+String.format("%03d",arr(state.get("sources")).size()+1),"name",name,"type",type,"sector",field(input,"sector",90),"frequencyHz",hz,"notes",field(input,"notes",600),"active",true);
      arr(state.get("sources")).add(o);event("success","Fuente electromagnética registrada: "+name);return o;
    }
    if(p.equals("/api/measurements")){
      authorized(actor,"tecnico");String hive=need(input,"hiveId",50);checkHive(hive);String type=need(input,"field",4);if(!is(type,"B","E"))throw new IllegalArgumentException("Magnitud no admitida.");
      double value=num(input.get("value"),Double.NaN),hz=num(input.get("frequencyHz"),Double.NaN);if(!Double.isFinite(value)||value<0||value>1e9||!Double.isFinite(hz)||hz<0||hz>1e12)throw new IllegalArgumentException("Valor o frecuencia no válidos.");
      String instrument=need(input,"instrument",100);
      Object o=measurement(hive,type,round(value,6),type.equals("B")?"µT":"V/m",(int)hz,"manual",instrument,null,field(input,"notes",500));
      arr(state.get("measurements")).add(o);event("success","Medición registrada manualmente en "+hive+" (no verificada automáticamente).");return o;
    }
    if(p.equals("/api/observations")){
      authorized(actor,"apicultor","tecnico","investigador");String hive=need(input,"hiveId",50);checkHive(hive);
      int entered=integer(input.get("entered"),-1),exited=integer(input.get("exited"),-1);double minutes=num(input.get("minutes"),-1);if(entered<0||exited<0||entered>1000000||exited>1000000||minutes<=0||minutes>1440)throw new IllegalArgumentException("Conteos o duración inválidos.");
      Object o=obj("id",unique("OBS"),"hiveId",hive,"entered",entered,"exited",exited,"minutes",minutes,"temperature",nullableNum(input.get("temperature"),-30,80),"humidity",nullableNum(input.get("humidity"),0,100),"weather",field(input,"weather",80),"flowers",field(input,"flowers",40),"notes",field(input,"notes",1000),"origin","manual-demo","actor",actor,"at",now());
      arr(state.get("observations")).add(o);event("success","Nueva observación apícola registrada en "+hive+" por "+actor+".");return o;
    }
    if(p.equals("/api/notes")){
      authorized(actor,"investigador");String hive=need(input,"hiveId",50);checkHive(hive);Object o=obj("id",unique("ANA"),"hiveId",hive,"title",need(input,"title",120),"text",need(input,"text",2000),"actor",actor,"at",now(),"origin","nota de demostración");arr(state.get("notes")).add(o);event("success","Nota de análisis registrada por el investigador.");return o;
    }
    if(p.equals("/api/tasks")){
      authorized(actor,"tecnico","apicultor");String hive=need(input,"hiveId",50);checkHive(hive);String st=field(input,"status",30);if(st.isBlank())st="pendiente";if(!is(st,"pendiente","en proceso","completada"))throw new IllegalArgumentException("Estado inválido");
      Object o=obj("id",unique("SEG"),"hiveId",hive,"title",need(input,"title",140),"details",field(input,"details",1000),"status",st,"actor",actor,"at",now());arr(state.get("tasks")).add(o);event("success","Nueva acción preventiva o seguimiento registrado.");return o;
    }
    if(p.equals("/api/tasks/status")){
      authorized(actor,"apicultor","tecnico");String id=need(input,"id",50),st=need(input,"status",30);if(!is(st,"pendiente","en proceso","completada"))throw new IllegalArgumentException("Estado inválido");for(Object o:arr(state.get("tasks"))){Map<String,Object> t=map(o);if(str(t.get("id")).equals(id)){t.put("status",st);t.put("updatedBy",actor);t.put("updatedAt",now());return t;}}throw new IllegalArgumentException("Seguimiento no encontrado.");
    }
    throw new IllegalArgumentException("Operación no encontrada: "+p);
  }
  static Object nullableNum(Object x,double min,double max){if(x==null||str(x).isBlank())return null;double n=num(x,Double.NaN);if(!Double.isFinite(n)||n<min||n>max)throw new IllegalArgumentException("Variable ambiental inválida");return round(n,2);}
  static void stream(HttpExchange x) throws IOException{
    Headers h=x.getResponseHeaders();h.set("Content-Type","text/event-stream; charset=utf-8");h.set("Cache-Control","no-cache, no-transform");h.set("Connection","keep-alive");x.sendResponseHeaders(200,0);
    OutputStream os=x.getResponseBody();subscribers.add(os);
    try{os.write(": conectado\n\n".getBytes(StandardCharsets.UTF_8));os.flush();while(running){Thread.sleep(12000);os.write(": keepalive\n\n".getBytes(StandardCharsets.UTF_8));os.flush();}}catch(Exception ignored){}finally{subscribers.remove(os);try{os.close();}catch(Exception ignored){}}
  }
  static void exportCsv(HttpExchange x) throws IOException{
    String[] hdr={"id","fecha_utc","colmena","magnitud","valor","unidad","frecuencia_hz","procedencia","instrumento","sesion"};
    StringBuilder sb=new StringBuilder("\ufeff"+String.join(";",hdr)+"\r\n");synchronized(LOCK){for(Object o:arr(state.get("measurements"))){Map<String,Object> m=map(o);String[] columns={str(m.get("id")),str(m.get("at")),str(m.get("hiveId")),str(m.get("field")),str(m.get("value")),str(m.get("unit")),str(m.get("frequencyHz")),str(m.get("origin")),str(m.get("instrument")),str(m.get("runId"))};for(int i=0;i<columns.length;i++)columns[i]="\""+columns[i].replace("\"","\"\"")+"\"";sb.append(String.join(";",columns)).append("\r\n");}}
    byte[] bytes=sb.toString().getBytes(StandardCharsets.UTF_8);x.getResponseHeaders().set("Content-Type","text/csv; charset=utf-8");x.getResponseHeaders().set("Content-Disposition","attachment; filename=beesafe_mediciones.csv");x.sendResponseHeaders(200,bytes.length);try(OutputStream out=x.getResponseBody()){out.write(bytes);}
  }
  static void staticFile(HttpExchange x,String raw) throws IOException{
    String p=raw.equals("/")?"/index.html":raw;String decoded=URLDecoder.decode(p,StandardCharsets.UTF_8);
    Path f=WEB.resolve(decoded.substring(1)).normalize();if(!f.startsWith(WEB)){fail(x,403,"Ruta no permitida.");return;}
    if(!Files.isRegularFile(f)){if(!decoded.contains("."))f=WEB.resolve("index.html");else{fail(x,404,"Recurso no encontrado.");return;}}
    String file=f.toString().toLowerCase();String type=file.endsWith(".html")?"text/html; charset=utf-8":file.endsWith(".css")?"text/css; charset=utf-8":file.endsWith(".js")?"application/javascript; charset=utf-8":file.endsWith(".png")?"image/png":file.endsWith(".svg")?"image/svg+xml":file.endsWith(".json")?"application/json":file.endsWith(".woff2")?"font/woff2":"application/octet-stream";
    byte[] bytes=Files.readAllBytes(f);x.getResponseHeaders().set("Content-Type",type);x.getResponseHeaders().set("X-Content-Type-Options","nosniff");x.getResponseHeaders().set("Cache-Control",file.endsWith(".html")?"no-cache":"public, max-age=3600");x.sendResponseHeaders(200,bytes.length);try(OutputStream out=x.getResponseBody()){out.write(bytes);}
  }
  /** Analizador y serializador JSON autocontenido: sin dependencias y con límites de entrada HTTP. */
  static class Json{
    final String s;int i=0;Json(String s){this.s=s;}
    static Object read(String s){Json p=new Json(s);Object v=p.value();p.ws();if(p.i!=s.length())throw new IllegalArgumentException("JSON sobrante");return v;}
    static String write(Object v){StringBuilder b=new StringBuilder();encode(b,v);return b.toString();}
    static void encode(StringBuilder b,Object v){
      if(v==null){b.append("null");return;}if(v instanceof Number || v instanceof Boolean){b.append(v);return;}
      if(v instanceof Map<?,?>){b.append('{');boolean first=true;for(Map.Entry<?,?> e:((Map<?,?>)v).entrySet()){if(!first)b.append(',');first=false;encode(b,String.valueOf(e.getKey()));b.append(':');encode(b,e.getValue());}b.append('}');return;}
      if(v instanceof Iterable<?>){b.append('[');boolean first=true;for(Object o:(Iterable<?>)v){if(!first)b.append(',');first=false;encode(b,o);}b.append(']');return;}
      b.append('"');for(char c:str(v).toCharArray()){switch(c){case '"':b.append("\\\"");break;case '\\':b.append("\\\\");break;case '\n':b.append("\\n");break;case '\r':b.append("\\r");break;case '\t':b.append("\\t");break;default:if(c<32)b.append(String.format("\\u%04x",(int)c));else b.append(c);}}b.append('"');
    }
    void ws(){while(i<s.length()&&Character.isWhitespace(s.charAt(i)))i++;}
    Object value(){ws();if(i>=s.length())throw new IllegalArgumentException("JSON incompleto");char c=s.charAt(i);if(c=='{')return object();if(c=='[')return array();if(c=='"')return quoted();if(s.startsWith("true",i)){i+=4;return true;}if(s.startsWith("false",i)){i+=5;return false;}if(s.startsWith("null",i)){i+=4;return null;}return number();}
    Map<String,Object> object(){i++;Map<String,Object> m=new LinkedHashMap<>();ws();if(i<s.length()&&s.charAt(i)=='}'){i++;return m;}while(true){ws();String key=quoted();ws();expect(':');m.put(key,value());ws();if(s.charAt(i)=='}'){i++;return m;}expect(',');}}
    List<Object> array(){i++;List<Object> a=new ArrayList<>();ws();if(i<s.length()&&s.charAt(i)==']'){i++;return a;}while(true){a.add(value());ws();if(s.charAt(i)==']'){i++;return a;}expect(',');}}
    String quoted(){expect('"');StringBuilder b=new StringBuilder();while(i<s.length()){char c=s.charAt(i++);if(c=='"')return b.toString();if(c!='\\'){b.append(c);continue;}if(i>=s.length())throw new IllegalArgumentException("Escape incompleto");char e=s.charAt(i++);switch(e){case '"':b.append('"');break;case '\\':b.append('\\');break;case '/':b.append('/');break;case 'n':b.append('\n');break;case 'r':b.append('\r');break;case 't':b.append('\t');break;case 'b':b.append('\b');break;case 'f':b.append('\f');break;case 'u':if(i+4>s.length())throw new IllegalArgumentException("Unicode truncado");b.append((char)Integer.parseInt(s.substring(i,i+4),16));i+=4;break;default:throw new IllegalArgumentException("Escape inválido");}}throw new IllegalArgumentException("Texto sin cerrar");}
    Number number(){int start=i;while(i<s.length()&&"-+.0123456789eE".indexOf(s.charAt(i))>=0)i++;if(i==start)throw new IllegalArgumentException("Número inválido");String v=s.substring(start,i);try{if(v.indexOf('.')>=0||v.toLowerCase().contains("e"))return Double.parseDouble(v);return Long.parseLong(v);}catch(Exception e){throw new IllegalArgumentException("Número inválido");}}
    void expect(char c){ws();if(i>=s.length()||s.charAt(i)!=c)throw new IllegalArgumentException("Se esperaba '"+c+"'");i++;}
  }
}
