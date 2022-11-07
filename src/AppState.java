public class AppState {
    private boolean isCopyWithThisAppJustBefore = false;

    public boolean isCopyWithThisAppJustBefore() {
        return isCopyWithThisAppJustBefore;
    }

    public void setCopyWithThisAppJustBefore(boolean copyWithThisAppJustBefore) {
        isCopyWithThisAppJustBefore = copyWithThisAppJustBefore;
    }
}
