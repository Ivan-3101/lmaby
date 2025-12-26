package com.DronaPay.frm.AMLCASES;

import java.util.List;

import lombok.Data;

@Data
public class GetUserMappingRequest {
    private String workflowKey;
    private String parentUserName;
    private List<String> parentUserGroupID;
    private List<String> childUserGroupID;
    private String childUserName;
}
