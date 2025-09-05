package de.julianweinelt.tobbql.parser;

import de.julianweinelt.tobbql.api.Query;
import de.julianweinelt.tobbql.data.Project;
import lombok.Getter;

import javax.swing.tree.DefaultMutableTreeNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

@Getter
public class Connection {
    private final Project project;

    public Connection(Project project) {
        this.project = project;
    }

    public boolean testConnection() {
        HttpClient client = HttpClient.newHttpClient();

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(createRequestURL("api/test-connection")))
                    .GET()
                    .header("TobbQL", "Client-V-" + "1.0")
                    .build();

            HttpResponse<String> response = sendRequest(client, request, HttpResponse.BodyHandlers.ofString());
            if (response != null) {
                return response.statusCode() == 200;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public List<String> getDatabases() {
        QueryResult result = query(Query.buildGetDatabases());
        List<String> databases = new ArrayList<>();
        for (Object o : result.getValuesOfColumn("dbName")) {
            if (o instanceof String dbName) {
                databases.add(dbName);
            }
        }
        return databases;
    }
    public List<String> getTables(String database) {
        return null;
    }
    public List<String> getColumns(String database, String table) {
        return null;
    }

    private QueryResult query(String query) {
        HttpClient client = HttpClient.newHttpClient();

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(createRequestURL("api/test-connection")))
                    .POST(HttpRequest.BodyPublishers.ofString(""))
                    .header("TobbQL", "Client-V-" + "1.0")
                    .build();

            HttpResponse<String> response = sendRequest(client, request, HttpResponse.BodyHandlers.ofString());
            if (response != null) {
                return new QueryResult(response.body());
            }
            return QueryResult.EMPTY_RESULT;
        } catch (Exception e) {
            return QueryResult.EMPTY_RESULT;
        }
    }

    private HttpResponse<String> sendRequest(HttpClient client, HttpRequest request, HttpResponse.BodyHandler<String> bodyHandler) {
        try {
            return client.send(request, bodyHandler);
        } catch (Exception e) {
            return null;
        }
    }

    public void createTree(DefaultMutableTreeNode root) {
        List<String> databases = getDatabases();
        DefaultMutableTreeNode dbNode = new DefaultMutableTreeNode("Datenbank");
        dbNode.add(new DefaultMutableTreeNode("Tabelle A"));
        dbNode.add(new DefaultMutableTreeNode("Tabelle B"));
    }

    private String createRequestURL(String endpoint) {
        return "http://" + project.getServer() + "/" + endpoint;
    }
}
