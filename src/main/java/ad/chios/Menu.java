package ad.chios;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import static com.mongodb.client.model.Sorts.descending;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bson.Document;
import org.bson.conversions.Bson;

/**
 *
 * @author David Pardo
 */

//Clase principal da nosa aplicación na que están contidos os métodos para traballar con ela
public class Menu {    
    private static BaseDatos bd;
    private static Scanner teclado = new Scanner(System.in);
    private static MongoClient mongo;
    private static File datos = new File("config.json");
    private static String entrada, nome, username, contrasinal, hashtag;
    private static final String  PATTERN = "#[a-zA-Zñáéíóúü]+";
    private static MongoCollection colUsuarios, colMensaxes;
    private static MongoDatabase mdb;
    private static List<String> arrayHashtags = new ArrayList<String>();
    private static List<DBObject> arrayFollows = new ArrayList<DBObject>();
    private static List<String> arrayFol = new ArrayList<String>();
    private static List<Document> list = new ArrayList<Document>();
    private static int i;
    
    //Método main dende o que configuramos a conexión á BD e chamamos ao menú 
    //de loguin
    public static void main(String args[]){                     
        confBD();        
        try{            
            mongo = new MongoClient(new MongoClientURI("mongodb://"+bd.getUsername()
                +":"+bd.getPassword()+"@"+bd.getAddress()+":"+bd.getPort()+ "/" 
                + bd.getDbname() + "?retryWrites=false"));
            //mongo = new MongoClient(bd.getAddress(),bd.getPort());
            mdb = mongo.getDatabase(bd.getDbname());
            colUsuarios = mdb.getCollection("usuario");
            colMensaxes = mdb.getCollection("mensaxe");
        }catch (Exception e){
            System.err.println(e);
        }                            
        
        menuLoguin();
            
    }
    
    //Método para a carga dos datos de conexión á BD
    public static void confBD(){
        if(datos.exists()){

            try{     
                FileReader fluxoDatos = new FileReader(datos);                
                BufferedReader buferEntrada = new BufferedReader(fluxoDatos);

                StringBuilder jsonBuilder = new StringBuilder();
                String linea;

                while ((linea=buferEntrada.readLine()) != null) {
                    jsonBuilder.append(linea).append("\n");                    
                }
                String json = jsonBuilder.toString();

                Gson gson = new Gson(); 

                bd = gson.fromJson(json, BaseDatos.class); 
                
            }catch (JsonSyntaxException | IOException e){
                System.err.println(e);
            }    

        }else {
            System.out.println("No existe el fichero de configuración de la bd.");
        } 
    }    
    
    //Segunda opción do menú loguin, mediante a cal rexistramos a un novo usuario 
    public static void rexistro(){
        
        System.out.println("Rexistro\n");
        System.out.println("Introduza o seu nome completo:\n");
            nome = teclado.nextLine(); 
        System.out.println("Introduza un username:\n");
            username = teclado.nextLine();
            
        while(consultaUsername(username)){
            System.out.println("Username en uso, introuza outro.\n");
            username = teclado.nextLine();
        }
                            
        System.out.println("Introsuza un contrasinal:\n");
            contrasinal = teclado.nextLine();               
                
        Document usuario = new Document()
            .append("nome", nome)
            .append("username", username)
            .append("password", contrasinal);

        colUsuarios.insertOne(usuario);    
        arrayFollows.clear();
        System.out.println("Usuario rexistrado.\n");
    }
    
    //Método chamado dende rexistro() mediante o cal facemos unha consulta á BD 
    //para comprobar se xa existe o username
    public static boolean consultaUsername(String username){        
        
        Document findDocument = new Document("username", username);
        MongoCursor<Document> cursor = colUsuarios.find(findDocument).iterator();
        
        if (cursor.hasNext()){             
            return true;            
        }
        return false;                
    }       
    
    //Primeira opción do menú loguin coa cal os usuarios rexistrados poden acceder 
    //ao menú inicio
    public static void login(){
        System.out.println("Login:\n");
        
        System.out.println("Introduza o seu username:\n");
        username= teclado.nextLine(); 

        if(!consultaUsername(username)){
            System.out.println("\nUsername non atopado.\n");
            menuLoguin();
        }

        System.out.println("Introduza o contrasinal:\n");
        contrasinal = teclado.nextLine();

        while(!consultaContrasinal(username, contrasinal)){
            System.out.println("Contrasinal incorrecto, tenteo de novo:\n");
            contrasinal = teclado.nextLine();
        }     
        menuInicio(username);
    }
    
    //Método chamado dende login() para comprobar que se inxeriron ben os datos de acceso
    public static boolean consultaContrasinal(String username, String contrasinal){
        BasicDBObject andQuery = new BasicDBObject();
        List<BasicDBObject> obj = new ArrayList<>();
	obj.add(new BasicDBObject("username", username));
	obj.add(new BasicDBObject("password", contrasinal));
	andQuery.put("$and", obj);
        
        MongoCursor<Document> result = colUsuarios.find(andQuery).iterator();
        while (result.hasNext()){             
            return true;
        }
        return false;    
    }
    
    //Menú inicio dende o cal elixiremos que vamos a facer
    public static void menuInicio(String username){
                
        while(true){
            System.out.println("\nMenú inicio.\n");
            System.out.println("\t1.Ver tódalas mensaxes.\n");
            System.out.println("\t2.Ver mensaxes de usuarios que sigo.\n");
            System.out.println("\t3.Buscar por hastag.\n");
            System.out.println("\t4.Escribir unha mensaxe.\n");
            System.out.println("\t5.Buscar usuarios.\n");
            System.out.println("\t6.Saír ao menú de loguin.\n");

            entrada = teclado.nextLine();
                        
            switch(entrada){
                case "1":
                    listarMensaxes();                     
                    break;
                case "2":
                    mensaxesFollows(username);
                    break;
                case "3":                    
                    buscarHastag();
                    break;
                case "4":
                    escribirmensaxe();
                    break;
                case "5":
                    buscarUsuario(username);
                    break;
                case "6":
                    menuLoguin();
                    break;    
                default:
                    System.out.println("Opción inválida.\n");
                    break;
            }                         
        }
    }
    
    //Metodo para a obtención da data actual
    public static Date getDate(){      
        Date now = new Date();
        BasicDBObject timeNow = new BasicDBObject("date", now); 
        return now;
    }
    
    //Método para obter o nome dun usuario mediante o seu username
    public static String consultaNome(String username){
        Document findDocument = new Document("username", username);
        MongoCursor<Document> cursor = colUsuarios.find(findDocument).iterator();
        String nameAux = null;
        try{
            while (cursor.hasNext()){             
                nameAux = cursor.next().get("nome").toString();                  
            }
        }finally{
            cursor.close();
        }
        return nameAux;  
    }
    
    //Método mediante o que podremos escribir unha mensaxe e inxerila na BD
    public static void escribirmensaxe(){                
        System.out.println("Agora escriba a mensaxe e pulse enter ao rematar.\n");
        String mensaxeEscrita = teclado.nextLine();                    
        Pattern pattern = Pattern.compile(PATTERN);
        
        Matcher matcher = pattern.matcher(mensaxeEscrita);
        
        while(matcher.find()){
            arrayHashtags.add(matcher.group().substring(1));            
        }
        
        Document mensaxe = new Document()
                .append("text", mensaxeEscrita)
                .append("user", new BasicDBObject()
                        .append("nome", consultaNome(username))
                        .append("username", username))
                .append("date", getDate())
                .append("hashtags", arrayHashtags);
                
        colMensaxes.insertOne(mensaxe);
        arrayHashtags.clear();
        System.out.println("\nMensaxe escrita correctamente.\n");
    }
    
    //Método que lista todas as mensaxes contidas na BD
    public static void listarMensaxes(){  
        MongoCursor<Document> cursor  = colMensaxes.find().sort(descending("date")).iterator();
        while(cursor.hasNext()){
            list.add(cursor.next());                        
        }
        
        paxinar();
        
        cursor.close();
    }
    
    //Método mediante o que podemos buscar mensaxes mediante un hashtag
    public static void buscarHastag(){         
        System.out.println("\nIntroduza o hastag a buscar:\n");
        hashtag = teclado.nextLine();
        while(!hashtag.matches(PATTERN)){
            System.out.println("\nFormato incorrecto, escribao de novo.\n");
            hashtag = teclado.nextLine();
        }        
        
        String fhashtag = hashtag.substring(1);
        
        Document query = new Document("hashtags", fhashtag);  
        MongoCursor<Document> cursor  = colMensaxes.find(query).iterator();
        while(cursor.hasNext()){
            list.add(cursor.next());                        
        }
            
        paxinar();
                
        cursor.close();
        
    }
    
    //Menú loguin mediante o que podemos rexistrarnos, acceder ou saír da aplicación
    public static void menuLoguin(){
        System.out.println("Benvido a Chíos.\n");        
        while(true){
            
            System.out.println("Introduza o número da opción:\n");
            System.out.println("\t1.Loguearse.\n");
            System.out.println("\t2.Rexistrarse.\n");
            System.out.println("\t3.Saír.\n");

            entrada = teclado.nextLine();
                        
            switch(entrada){
                case "1":
                    login();
                    break;
                case "2":
                    rexistro();
                    break;
                case "3":
                    System.exit(0);
                default:
                    System.out.println("Opción inválida.\n");
                    break;
            }           
        }
    }

    //Método que lista as mensaxes das persoas que sigue o usuario actual
    private static void mensaxesFollows(String username) {
        Document query = new Document("username", username);
        MongoCursor<Document> cursor  = colUsuarios.find(query).iterator();
        while (cursor.hasNext()){
            arrayFollows = (List<DBObject>) cursor.next().get("follows");            
        }
        cursor.close();
        
        if(arrayFollows == null){
            System.out.println("Non se sigue a ningún usuario.");
            return;
        }           
                
        for(i = 0;i < arrayFollows.size();i++){
            query = new Document("user.username", arrayFollows.get(i));
            cursor  = colMensaxes.find(query).iterator();
            while(cursor.hasNext()){
                list.add(cursor.next());                        
            }
            
            paxinar();
        }   
        
        arrayFollows.clear();
        cursor.close();
    }

    //Método mediante o que podemos buscar usuarios na BD mediante coincidencia 
    //total ou parcialco seu username
    private static void buscarUsuario(String username) {
        System.out.println("Indique o username de usuario a procurar:\n");
        String user = teclado.nextLine();
        
        Document regexQuery = new Document();
        regexQuery.put("username", 
            new BasicDBObject("$regex", ".*" + user + ".*")
            .append("$options", "i"));
        
        MongoCursor<Document> cursor = colUsuarios.find(regexQuery).iterator();
        
        while (cursor.hasNext()) {
            arrayFol.add(cursor.next().get("username").toString());
        }   
        
        if(arrayFol.size() > 0){
            System.out.println("\nUsuarios atopados:\n");
            for(i = 0;i < arrayFol.size();i++){                
                System.out.println(i + "." + arrayFol.get(i));
            }     
            System.out.println("\nElixa o número do usuario que quere comezar a seguir:\n");
            try{
                i = Integer.parseInt(teclado.nextLine());
            }catch(NumberFormatException nfe){
                System.out.println("\nValor incorrecto. Volvendo ó menú inicio...\n");
                return;
            }
            
            if(i <= arrayFol.size()){ 
                Bson query = new Document("username",username);
                Bson list = new BasicDBObject("follows",arrayFol.get(i));
                Bson updateQuery = new BasicDBObject("$push", list);
                colUsuarios.updateOne(query, updateQuery);
                System.out.println("\nUsuario engadido para o seu seguimento....\n");
            }
        }else{
            System.out.println("\nNon se atoparon usuarios que coincidan.\n");
        }
        arrayFol.clear();
    }         
    
    //Método para paxinar
    private static void paxinar(){        
        int x = 0;
        int y = 5;

        while(true){
            if(y > list.size()){
                y = list.size();
            }

            for(i = (0 + x);i< y;i++){
                Document bdo = list.get(i);
                Document object = (Document) bdo.get("user");

                System.out.println("\n" + object.getString("username"));
                System.out.println(object.getString("nome"));
                System.out.println(bdo.getString("text"));
                System.out.println(bdo.getDate("date") + "\n");
            }                        

            if(y == list.size()){
                System.out.println("Non hai máis mensaxes que mostrar.");
                break;
            }

            x = x + 5;
            y = y + 5;

            System.out.println("Pulse calquer tecla para ver máis mensaxes.");
            teclado.nextLine();
        } 
        list.clear();
    }
}
