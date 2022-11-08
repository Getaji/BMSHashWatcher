import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Config {
    private final List<WebService> webServiceList = new ArrayList<>();

    private String beatorajaPath = "";

    @JsonProperty("webServiceList")
    public List<WebService> getWebServiceList() {
        return webServiceList;
    }

    public void setWebServiceList(List<WebService> list) {
        webServiceList.clear();
        webServiceList.addAll(list);
    }

    @JsonProperty("beatorajaPath")
    public String getBeatorajaPath() {
        return beatorajaPath;
    }

    public void setBeatorajaPath(String beatorajaPath) {
        this.beatorajaPath = beatorajaPath;
    }

    public static Config load(String pathname) throws IOException {
        final File file = new File(pathname);
        if (file.exists()) {
            // TODO validate
            final String text = Files.readString(file.toPath());
            final ObjectMapper objectMapper = new ObjectMapper();
            final Config config = objectMapper.readValue(text, new TypeReference<>() {
            });
            return config;
        } else {
            final Config config = new Config();
            Collections.addAll(
                    config.getWebServiceList(),
                    new WebService("Mocha-Repository", "", "https://mocha-repository.info/song.php?sha256=%s"),
                    new WebService("MinIR", "", "https://www.gaftalk.com/minir/#/viewer/song/%s/0"),
                    new WebService("Cinnamon", "", "https://cinnamon.link/charts/%s"),
                    new WebService("LR2IR", "http://www.dream-pro.info/~lavalse/LR2IR/search.cgi?mode=ranking&bmsmd5=%s", "")
            );
            final ObjectMapper objectMapper = new ObjectMapper();
            final String text = objectMapper.writeValueAsString(config);
            Files.writeString(file.toPath(), text);
            return config;
        }
    }

    public static void save(String pathname, Config config) throws IOException {
        final ObjectMapper objectMapper = new ObjectMapper();
        final String text = objectMapper.writeValueAsString(config);
        Files.writeString(Path.of(pathname), text);
    }
}
