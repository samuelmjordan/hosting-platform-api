package com.mc_host.api.service.resources.v2.context;

public enum ContextField {
    NODE_ID("node_id"),
    A_RECORD_ID("a_record_id"),
    PTERODACTYL_NODE_ID("pterodactyl_node_id"),
    ALLOCATION_ID("allocation_id"),
    PTERODACTYL_SERVER_ID("pterodactyl_server_id"),
    C_NAME_RECORD_ID("c_name_record_id"),
    NEW_NODE_ID("new_node_id"),
    NEW_A_RECORD_ID("new_a_record_id"),
    NEW_PTERODACTYL_NODE_ID("new_pterodactyl_node_id"),
    NEW_ALLOCATION_ID("new_allocation_id"),
    NEW_PTERODACTYL_SERVER_ID("new_pterodactyl_server_id"),
    NEW_C_NAME_RECORD_ID("new_c_name_record_id");
    
    private final String columnName;
    
    ContextField(String columnName) {
        this.columnName = columnName;
    }
    
    public String getColumnName() {
        return columnName;
    }
}
