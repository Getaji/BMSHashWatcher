package com.getaji.bmshashwatcher.model;

/**
 * アプリケーションの状態を管理するクラス
 * Configに保存しないものがここで管理される
 */
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
