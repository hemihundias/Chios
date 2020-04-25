/*
 * 
 * https://github.com/hemihundias/Chios
 * 
 */
package ad.chios;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.model.DBCollectionFindOptions;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Sorts;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bson.BsonDocument;
import org.bson.conversions.Bson;

/**
 *
 * @author Hemihundias
 */

public class Menu {    
    private static BaseDatos bd;
    private static Scanner teclado = new Scanner(System.in);
    private static MongoClient mongo;
    private static File datos = new File("config.json");
    private static String entrada, nome, username, contrasinal, text, hashtag, follow;
    private static final String  PATTERN = "#[a-zA-Zñáéíóúü]+";
    private static DBCollection colUsuarios, colMensaxes;
    private static DB mdb;
    private static List<String> arrayHashtags = new ArrayList<String>();
    private static List<DBObject> arrayFollows = new ArrayList<DBObject>();
    private static List<String> arrayFol = new ArrayList<String>();
    private static int i;
    
    public static void main(String args[]){                     
        confBD();
        
        try{
            mongo = new MongoClient(bd.getAddress(),bd.getPort());
            mdb = mongo.getDB(bd.getDbname());
            colUsuarios = mdb.getCollection("usuarios");
            colMensaxes = mdb.getCollection("mensaxes");
        }catch (Exception e){
            
        }
        
        menuLoguin();
            
    }
    
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
                
        DBObject usuario = new BasicDBObject()
            .append("nome", nome)
            .append("username", username)
            .append("contrasinal", contrasinal);

        colUsuarios.insert(usuario);    
        arrayFollows.clear();
        System.out.println("Usuario rexistrado.\n");
    }
    
    public static boolean consultaUsername(String username){   
        DBObject query = new BasicDBObject("username",username );
        DBCursor cursor = colUsuarios.find(query);
        if (cursor.hasNext()){             
            return true;            
        }
        return false;                
    }       
    
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
    
    public static boolean consultaContrasinal(String username, String contrasinal){
        BasicDBObject andQuery = new BasicDBObject();
        List<BasicDBObject> obj = new ArrayList<>();
	obj.add(new BasicDBObject("username", username));
	obj.add(new BasicDBObject("contrasinal", contrasinal));
	andQuery.put("$and", obj);
        
        DBCursor cursor = colUsuarios.find(andQuery);
        while (cursor.hasNext()){             
            return true;
        }
        return false;    
    }
    
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
    
    public static String getDate(){      
        DateFormat df = new SimpleDateFormat( "EEE, d MMM yyyy HH:mm:ss z", Locale.ENGLISH);
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        return df.format(new Date());
    }
    
    public static String consultaNome(String username){
        DBObject query = new BasicDBObject("username",username );
        DBCursor cursor = colUsuarios.find(query);
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
    
    public static void escribirmensaxe(){                
        System.out.println("Agora escriba a mensaxe e pulse enter ao rematar.\n");
        String mensaxeEscrita = teclado.nextLine();                    
        Pattern pattern = Pattern.compile(PATTERN);
        
        Matcher matcher = pattern.matcher(mensaxeEscrita);
        
        while(matcher.find()){
            arrayHashtags.add(matcher.group().substring(1));            
        }
        
        DBObject mensaxe = new BasicDBObject()
                .append("text", mensaxeEscrita)
                .append("user", new BasicDBObject()
                        .append("nome", consultaNome(username))
                        .append("username", username))
                .append("date", getDate())
                .append("hashtags", arrayHashtags);
        
        
        colMensaxes.insert(mensaxe);
        arrayHashtags.clear();
        System.out.println("\nMensaxe escrita correctamente.\n");
    }
    
    public static void listarMensaxes(){
        DBCollectionFindOptions options = new DBCollectionFindOptions();
        Bson projectionAux = Projections.include(Arrays.asList("user.nome","user.username","text","date"));
        DBObject projection = new BasicDBObject(projectionAux.toBsonDocument(BsonDocument.class, 
            MongoClient.getDefaultCodecRegistry()));
        options.projection(projection);
        options.limit(5);
        Bson sortAux = Sorts.descending("date");
        DBObject sort = new BasicDBObject(sortAux.toBsonDocument(BsonDocument.class, 
            MongoClient.getDefaultCodecRegistry()));
        options.sort(sort);
        DBCursor cursor  = colMensaxes.find(new BasicDBObject(),options);
        while (cursor.hasNext()){
            BasicBDObject dbo = (BasicDBObject) cursor.next();            
            DBObject object = (DBObject) dbo.get("user");
            
            System.out.println("\n" + object.get("username"));
            System.out.println(object.get("nome"));
            System.out.println(dbo.get("text")); 
            System.out.println(dbo.get("date") + "\n");
        }
        cursor.close();
    }
    
    public static void buscarHastag(){
                
        System.out.println("\nIntroduza o hastag a buscar:\n");
        hashtag = teclado.nextLine();
        while(!hashtag.matches(PATTERN)){
            System.out.println("\nFormato incorrecto, escribao de novo.\n");
            hashtag = teclado.nextLine();
        }        
        
        Bson filter = Filters.eq("hashtags", hashtag.substring(1));
        DBObject query = new BasicDBObject(filter.toBsonDocument(BsonDocument.class, 
                MongoClient.getDefaultCodecRegistry()));
        
        DBCursor cursor  = colMensaxes.find(query);
        while (cursor.hasNext()){
            DBObject documentoAux = cursor.next();
            System.out.println(documentoAux.toString());
        }
        cursor.close();
    }
    
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

    private static void mensaxesFollows(String username) {
        DBObject query = new BasicDBObject("username", username);
        DBCursor cursor  = colUsuarios.find(query);
        while (cursor.hasNext()){
            arrayFollows = (List<DBObject>) cursor.next().get("follows");            
        }
        cursor.close();
        
        for(i = 0;i < arrayFollows.size();i++){
            query = new BasicDBObject("user.username", arrayFollows.get(i));
            cursor  = colMensaxes.find(query);
            while (cursor.hasNext()){
                BasicDBObject dbo = (BasicDBObject) cursor.next();
                DBObject object = (DBObject) dbo.get("user");
                
                System.out.println("\n" + object.get("username"));
                System.out.println(object.get("nome"));
                System.out.println(dbo.get("text")); 
                System.out.println(dbo.get("date") + "\n");                 
            }
            cursor.close();
        }
        arrayFollows.clear();
    }

    private static void buscarUsuario(String username) {
        System.out.println("Indique o username de usuario a procurar:\n");
        String user = teclado.nextLine();
        
        BasicDBObject regexQuery = new BasicDBObject();
        regexQuery.put("username", 
            new BasicDBObject("$regex", ".*" + user + ".*")
            .append("$options", "i"));
        
        DBCursor cursor = colUsuarios.find(regexQuery);
        
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
                DBObject query = new BasicDBObject("username",username );
                DBObject list = new BasicDBObject("follows",arrayFol.get(i));
                DBObject updateQuery = new BasicDBObject("$push", list);
                colUsuarios.update(query, updateQuery);
                System.out.println("\nUsuario engadido para o seu seguimento....\n");
            }
        }else{
            System.out.println("\nNon se atoparon usuarios que coincidan.\n");
        }
        arrayFol.clear();
    }               
    
}
