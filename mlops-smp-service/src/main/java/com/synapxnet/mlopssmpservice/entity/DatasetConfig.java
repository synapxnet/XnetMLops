package com.synapxnet.mlopssmpservice.entity;


import lombok.Data;


@Data
public class DatasetConfig {
    private int id;
    private String config_type;
    private String label;
    private String value;


}
