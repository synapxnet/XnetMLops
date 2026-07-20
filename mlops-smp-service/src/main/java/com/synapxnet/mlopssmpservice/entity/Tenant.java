package com.synapxnet.mlopssmpservice.entity;

import lombok.Data;

@Data
public class Tenant {
    private String uid;
    private String tenantId;
    private String tenantName;
    private String status;
}
