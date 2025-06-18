package vciptvman.component;

public interface EpgdBufferCreator {
    public StringBuffer createExportEpgdBuffer(boolean addVdr, boolean addAllOthers);
}
