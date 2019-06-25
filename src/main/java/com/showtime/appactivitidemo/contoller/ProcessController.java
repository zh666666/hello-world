package com.showtime.appactivitidemo.contoller;

import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.repository.ProcessDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author: Baron
 * @Description: process流程相关controller
 * @Date: Created in 2019/6/25 9:45
 */
@RestController
@RequestMapping("/process")
@Slf4j
public class ProcessController {

    @Autowired
    private RepositoryService repositoryService;

    /**
     * 获取已经部署流程
     *
     * @return
     */
    @GetMapping("/list")
    public ResponseEntity getDeployProcess() {
        List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery().
                orderByProcessDefinitionVersion().desc().listPage(0, 10);
        List<Map<String, String>> list = new ArrayList<>(processDefinitions.size());
        for (ProcessDefinition processDefinition : processDefinitions) {
            Map map = new HashMap<>();
            map.put("id", processDefinition.getId());
            map.put("processDefinitionKey", processDefinition.getKey());
            map.put("name", processDefinition.getName());
            map.put("deploymentId", processDefinition.getDeploymentId());
            map.put("description", processDefinition.getDescription());
            list.add(map);
        }
        return ResponseEntity.ok(list);
    }

}
