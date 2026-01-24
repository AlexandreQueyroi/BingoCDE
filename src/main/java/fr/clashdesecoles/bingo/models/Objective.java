package fr.clashdesecoles.bingo.models;

import org.bukkit.Material;

public class Objective {
    
    private final int id;
    private final String internalName;
    private final String displayName;
    private final Material item;
    private final ObjectiveAction action;
    private final String actionCheck;
    private ObjectiveStatus status;
    
    public enum ObjectiveAction {
        KILL,
        ITEM,
        OBJECTIVE
    }
    
    public enum ObjectiveStatus {
        PENDING,
        SUCCESS,
        REJECTED
    }
    
    public Objective(int id, String internalName, String displayName, Material item, 
                     ObjectiveAction action, String actionCheck) {
        this.id = id;
        this.internalName = internalName;
        this.displayName = displayName;
        this.item = item;
        this.action = action;
        this.actionCheck = actionCheck;
        this.status = ObjectiveStatus.PENDING;
    }
    
    public int getId() {
        return id;
    }
    
    public String getInternalName() {
        return internalName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public Material getItem() {
        return item;
    }
    
    public ObjectiveAction getAction() {
        return action;
    }
    
    public String getActionCheck() {
        return actionCheck;
    }
    
    public ObjectiveStatus getStatus() {
        return status;
    }
    
    public void setStatus(ObjectiveStatus status) {
        this.status = status;
    }
}
