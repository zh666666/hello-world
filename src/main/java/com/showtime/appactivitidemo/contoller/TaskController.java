package com.showtime.appactivitidemo.contoller;

import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author: Baron
 * @Description: task流程实例controller
 * @Date: Created in 2019/6/25 10:03
 */
@RestController
@RequestMapping("/task")
@Slf4j
public class TaskController {

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private HistoryService historyService;


    /**
     * 开启流程实例
     *
     * @param processDefinitionKey 流程key
     * @param businessKey          事件id，这里用的是记录请假事件的id
     * @return
     */
    @PostMapping("/start")
    public ResponseEntity startTask(@RequestParam("processDefinitionKey") String processDefinitionKey,
                                    @RequestParam("businessKey") String businessKey) {
        Map<String, String> result = new ConcurrentHashMap<>(16);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(processDefinitionKey, businessKey);
        result.put("id", processInstance.getId());
        result.put("businessKey", processInstance.getBusinessKey());
        log.info("processInstance={}", processInstance);
        ProcessInstance result1 = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult();
        System.out.println(result1.equals(processInstance));
        return ResponseEntity.ok(result);
    }

    /**
     * 员工完成请假提交的接口
     *
     * @param businessKey 请假事件id
     * @param assignee    委办人/向谁请假
     * @param condition   提交/取消（1为提交，0为取消）
     * @return
     */
    @PostMapping("/completeBySubmitter")
    public ResponseEntity completeTaskWithOrder(@RequestParam("businessKey") String businessKey,
                                                @RequestParam("assignee") String assignee,
                                                @RequestParam("condition") Integer condition) {
        Map<String, String> result = new ConcurrentHashMap<>(16);
        Task task = taskService.createTaskQuery().
                processInstanceBusinessKeyLike(businessKey).singleResult();
        log.info("task={}", task);
        result.put("taskId", task.getId());
        //设置流转条件
        Map<String, Object> variables = new ConcurrentHashMap<>(16);
        variables.put("type", condition);
        taskService.complete(task.getId(), variables);
        Task task1 = taskService.createTaskQuery().
                processInstanceBusinessKeyLike(businessKey).singleResult();
        taskService.setAssignee(task1.getId(), assignee);
        return ResponseEntity.ok(result);
    }


    /**
     * 根据指定委办人，查询代办事件
     *
     * @param assignee 委办人的标识
     * @return
     */
    @GetMapping("/toDoTasksByAssignee")
    public ResponseEntity getToDoTaskList(String assignee) {
        List<Map<String, Object>> list = null;
        //指定人
        List<Task> listByAssignee = taskService.createTaskQuery().taskAssignee(assignee).list();
        if (listByAssignee != null && listByAssignee.size() > 0) {
            list = new ArrayList<>(listByAssignee.size());
            for (Task task : listByAssignee) {
                Map<String, Object> map = new ConcurrentHashMap<>();
                map.put("taskAssignee", task.getAssignee());
                map.put("taskId", task.getId());
                map.put("taskProcessInstanceId", task.getProcessInstanceId());
                list.add(map);
            }
        }
        return ResponseEntity.ok(list);
    }

    /**
     * 委办人完成自己流转到自己这一环节的任务
     *
     * @param businessKey 业务id
     * @param handler     处理人
     * @param assignee    下一个委办人
     * @param condition   流传条件
     * @return
     */
    @PostMapping("/completeByAssignee")
    public ResponseEntity completeTaskByAssigneeWithOrder(@RequestParam("businessKey") String businessKey,
                                                          @RequestParam("handler") String handler,
                                                          @RequestParam("assignee") String assignee,
                                                          @RequestParam("condition") Integer condition) {
        Map<String, String> result = new ConcurrentHashMap<>(16);
        Task task = taskService.createTaskQuery().
                processInstanceBusinessKeyLike(businessKey).singleResult();
        log.info("task={}", task);
        if (task == null) {
            result.put("msg", "执行失败，没有该任务！");
            return ResponseEntity.ok(result);
        }
        if (!handler.equals(task.getAssignee())) {
            result.put("msg", "执行失败，不是指定委办人没有权限");
            result.put("task.getAssignee()", task.getAssignee());
            return ResponseEntity.ok(result);
        }
        result.put("taskId", task.getId());
        //设置流转条件
        Map<String, Object> variables = new ConcurrentHashMap<>(16);
        variables.put("type", condition);
        taskService.complete(task.getId(), variables);
        Task task1 = taskService.createTaskQuery().
                processInstanceBusinessKeyLike(businessKey).singleResult();
        //如果流程还没完成，设置下一个委办人
        if (task1 != null) {
            taskService.setAssignee(task1.getId(), assignee);
        }
        return ResponseEntity.ok(result);
    }

    /**
     * 通过taskId校验操作人完成本人项目
     *
     * @param taskId    任务id
     * @param handler   操作人
     * @param assignee  下一个委办人
     * @param condition 流转条件
     * @return
     */
    @PostMapping("/completeByAssigneeAndTaskId")
    public ResponseEntity completeTaskByAssigneeWithTaskId(@RequestParam("taskId") String taskId,
                                                           @RequestParam("handler") String handler,
                                                           @RequestParam("assignee") String assignee,
                                                           @RequestParam("condition") Integer condition) {
        Map<String, String> result = new ConcurrentHashMap<>(16);
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            result.put("msg", "执行失败，没有该任务！");
            return ResponseEntity.ok(result);
        }
        if (!handler.equals(task.getAssignee())) {
            result.put("msg", "执行失败，不是指定委办人没有权限");
            result.put("task.getAssignee()", task.getAssignee());
            return ResponseEntity.ok(result);
        }
        String processDefinitionId = task.getProcessInstanceId();
        result.put("taskId", task.getId());
        //设置流转条件
        Map<String, Object> variables = new ConcurrentHashMap<>(16);
        variables.put("type", condition);
        taskService.complete(task.getId(), variables);
        Task nextTask = taskService.createTaskQuery().processInstanceId(processDefinitionId).singleResult();
        //如果流程还没完成，设置下一个委办人
        if (nextTask != null) {
            taskService.setAssignee(nextTask.getId(), assignee);
        }
        return ResponseEntity.ok(result);
    }

    /**
     * 根据businessKey获取操作信息
     *
     * @param businessKey 业务id
     * @return
     */
    @GetMapping("/processInstanceTaskHistory")
    public ResponseEntity getHistory(@RequestParam("businessKey") String businessKey) {
        List<HistoricTaskInstance> list = historyService.createHistoricTaskInstanceQuery().
                processInstanceBusinessKey(businessKey).list();
        for (HistoricTaskInstance historicTaskInstance : list) {
            //获取task评论和附件
            System.out.println(historicTaskInstance.getStartTime());
            taskService.getComment(historicTaskInstance.getId());
            taskService.getAttachment(historicTaskInstance.getId());
        }
        log.info("list={}", list);
        return ResponseEntity.ok(list);
    }


}
