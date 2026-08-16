package hahaha.talkblock;

record TalkblockEntry(String reason, String lastKnownName) {
    TalkblockEntry {
        reason = reason == null ? "" : reason;
        lastKnownName = lastKnownName == null ? "" : lastKnownName;
    }
}
