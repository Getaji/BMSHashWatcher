import javafx.beans.property.SimpleStringProperty;

public class MainWindowModel {
    private final SimpleStringProperty message = new SimpleStringProperty("");

    public String getMessage() {
        return message.get();
    }

    public SimpleStringProperty messageProperty() {
        return message;
    }
}
