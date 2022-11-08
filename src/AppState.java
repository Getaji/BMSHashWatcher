public class AppState {
    private boolean isCopyWithThisAppJustBefore = false;
    private boolean isFirstBoot = false;

    public boolean isCopyWithThisAppJustBefore() {
        return isCopyWithThisAppJustBefore;
    }

    public void setCopyWithThisAppJustBefore(boolean copyWithThisAppJustBefore) {
        isCopyWithThisAppJustBefore = copyWithThisAppJustBefore;
    }

    public boolean isFirstBoot() {
        return isFirstBoot;
    }

    public void setFirstBoot(boolean firstBoot) {
        isFirstBoot = firstBoot;
    }
}
