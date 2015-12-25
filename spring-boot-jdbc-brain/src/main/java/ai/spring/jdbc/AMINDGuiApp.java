package ai.spring.jdbc;

import java.io.PrintStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@EnableAutoConfiguration
@ComponentScan
@RestController("/")
public class AMINDGuiApp extends WebMvcConfigurerAdapter{

    public static final String NEO4J_URL = System.getProperty("NEO4J_URL","jdbc:neo4j://localhost:7474");

    public static final RowMapper<Person> PERSON_ROW_MAPPER = new RowMapper<Person>() {
        public Person mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Person(rs.getString("title"));
        }
    };

    // Template query: get movie
    @Autowired
    JdbcTemplate template;

//    String GET_MOVIE_QUERY =
//            "MATCH (movie:Movie {title:{1}})" +
//            " OPTIONAL MATCH (movie)<-[r]-(person:Person)\n" +
//            " RETURN movie.title as title, collect({name:person.name, job:head(split(lower(type(r)),'_')), role:r.roles}) as cast LIMIT 1";

    //Definition of the class MOVIE
    public static class Person {
        public String title;

        public Person(String title) {
            this.title = title;
        }
    }
    
    @RequestMapping("/search")
    public List<Person> search(@RequestParam("q") String query) {
        if (query==null || query.trim().isEmpty()) return Collections.emptyList();
        String queryParam = "(?i).*" + "AMIND" + ".*";
        System.out.print("FIRE/n");
        return template.query(query, PERSON_ROW_MAPPER, queryParam);
    }

    public static final String GRAPH_QUERY = "MATCH (m)<-[r]-(a) "+
    "RETURN r.roles as rel, "+
    "m.name as node2, "+
    "a.name as node1" +
            " LIMIT {1}";
   
    @RequestMapping("/graph")
    public Map<String, Object> graph(@RequestParam(value = "limit",required = false) Integer limit) {
        Iterator<Map<String, Object>> result = template.queryForList(
                GRAPH_QUERY, limit == null ? 100 : limit).iterator();
        return toD3Format(result);
    }
    

    //Links and Nodes creation. 
    private Map<String, Object> toD3Format(Iterator<Map<String, Object>> result) {
        List<Map<String,Object>> nodes = new ArrayList<Map<String,Object>>();
        List<Map<String,Object>> rels= new ArrayList<Map<String,Object>>();
              
        while (result.hasNext()) {

        	Map<String, Object> row = result.next();
  	
            //Create target i for Node2
            Map<String, Object> node2 = map("title", row.get("node2"),"label","nodeStyle");
            System.out.print(" | NODE2:" + !nodes.contains(node2));
            if(!nodes.contains(node2))
            nodes.add(node2);
            int target = nodes.indexOf(node2);
            
            //Create source i for Node1
            Map<String, Object> node1 = map("title", row.get("node1"),"label","nodeStyle");
            System.out.print(" | NODE1:" + !nodes.contains(node1));
            if(!nodes.contains(node1))
            nodes.add(node1);
            int source = nodes.indexOf(node1); 
            
            //Adding relationship to "rels"
            Map<String, Object> relationship = map("source",source,"target",target);
            relationship.put("relationship", row.get("rel"));
            rels.add(relationship);
        }
        //Make relation between Nodes and Links
        return map("nodes", nodes, "links", rels);
    }

    private Map<String, Object> map(String key1, Object value1, String key2, Object value2) {
        Map<String, Object> result = new HashMap<String,Object>(2);
        result.put(key1,value1);
        result.put(key2,value2);
        return result;
    }

    public static void main(String[] args) throws Exception {
        System.setErr(new PrintStream(System.out) {
            @Override
            public void write(int b) {
                super.write(b);
            }

            @Override
            public void write(byte[] buf, int off, int len) {
                super.write(buf, off, len);
            }
        });
        new SpringApplicationBuilder(AMINDGuiApp.class).run(args);
    }

    //Database connection with authentication
    @Bean
    public DataSource dataSource() {
        return new DriverManagerDataSource(NEO4J_URL,"neo4j","tietie666");
    }
}
