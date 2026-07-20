package com.synapxnet.mlopssmpservice.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeptTreeData {
    private String label;
    private String value;
    private List<DeptTreeData> children;
}
