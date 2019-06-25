package com.showtime.appactivitidemo.contoller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.activiti.bpmn.converter.BpmnXMLConverter;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.editor.constants.ModelDataJsonConstants;
import org.activiti.editor.language.json.converter.BpmnJsonConverter;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.Model;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author: Baron
 * @Description: 流程模型controller类
 * @Date: Created in 2019/6/19 23:24
 */
@RestController
@Slf4j
@RequestMapping("/model")
public class ModelController {

    @Autowired
    private RepositoryService repositoryService;

    /**
     * 获取流程模型列表
     * @param page
     * @param pageNum
     * @return
     */
    @GetMapping("/list")
    public ResponseEntity getModels(@RequestParam(required = false,defaultValue = "0") Integer page,
                                    @RequestParam(required = false,defaultValue = "10") Integer pageNum) {
        List<Model> models = repositoryService.createModelQuery().orderByCreateTime().desc().listPage(page, pageNum);
        return ResponseEntity.ok(models);
    }

    /**
     * 创建流程模型
     * @param request
     * @param response
     */
    @GetMapping("/create")
    public void create(HttpServletRequest request, HttpServletResponse response) {
        try {
            //设置默认值
            String name = "";
            String description = "";
            int version = 1;
            String key = "1";
            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode editorNode = objectMapper.createObjectNode();
            editorNode.put("id", "canvas");
            editorNode.put("resourceId", "canvas");
            ObjectNode stencilSetNode = objectMapper.createObjectNode();
            stencilSetNode.put("namespace", "http://b3mn.org/stencilset/bpmn2.0#");
            editorNode.put("stencilset", stencilSetNode);
            Model modelData = repositoryService.newModel();

            ObjectNode modelObjectNode = objectMapper.createObjectNode();
            modelObjectNode.put(ModelDataJsonConstants.MODEL_NAME, name);
            modelObjectNode.put(ModelDataJsonConstants.MODEL_REVISION, version);
            modelObjectNode.put(ModelDataJsonConstants.MODEL_DESCRIPTION, description);
            modelData.setMetaInfo(modelObjectNode.toString());
            modelData.setName(name);
            modelData.setKey(key);
            //保存模型
            repositoryService.saveModel(modelData);
            repositoryService.addModelEditorSource(modelData.getId(), editorNode.toString().getBytes("utf-8"));
            response.sendRedirect(request.getContextPath() + "/modeler.html?modelId=" + modelData.getId());
        } catch (Exception e) {
            log.error("流程创建失败，e={}",e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 通过id删除流程模型
     * @param modelId
     */
    @DeleteMapping("/remove")
    public ResponseEntity remove(@RequestParam("modelId") String modelId) {
        Map map = new HashMap<>(10);
        try {
            repositoryService.deleteModel(modelId);
            map.put("success", false);
            map.put("msg", "流程删除成功，modelId=" + modelId);
        } catch (Exception e) {
            map.put("success", false);
            map.put("msg", "流程不存在，modelId=" + modelId);
            log.error("e={}",e.getMessage());
            e.printStackTrace();
        }
        return ResponseEntity.ok(map);
    }

    /**
     * 流程模型部署
     * @param modelId
     */
    @PostMapping("/deploy/{modelId}")
    public ResponseEntity deploy(@PathVariable("modelId") String modelId) {
        Map map = new HashMap<>(10);
        Model model = repositoryService.getModel(modelId);
        if (model==null) {
            map.put("success",false);
            map.put("msg", "流程不存在，modelId="+modelId);
            log.error("流程不存在，modelId={}",modelId);
            return ResponseEntity.ok(map);
        }
        try {
            byte[] modelByte = repositoryService.getModelEditorSource(model.getId());
            if (modelByte == null) {
                map.put("success", false);
                map.put("msg","模型数据为空,请先设计好，再部署！");
                log.error("模型数据为空,请先设计好，再部署！modelId={}",modelId);
                return ResponseEntity.ok(map);
            }
            JsonNode modelNode = new ObjectMapper().readTree(modelByte);
            BpmnModel bpmnModel = new BpmnJsonConverter().convertToBpmnModel(modelNode);
            if (bpmnModel.getProcesses().size()==0) {
                map.put("success", false);
                map.put("msg","数据模型不符要求，请至少设计一条主线流程");
                log.error("数据模型不符要求，请至少设计一条主线流程");
                return ResponseEntity.ok(map);
            }
            byte[] bpmnBytes = new BpmnXMLConverter().convertToXML(bpmnModel);
            //发布流程
            String processName = model.getName() + ".bpmn20.xml";
            Deployment deployment = repositoryService.createDeployment()
                    .name(model.getName())
                    .addString(processName, new String(bpmnBytes, "UTF-8"))
                    .deploy();
            model.setDeploymentId(deployment.getId());
            repositoryService.saveModel(model);
            map.put("success", true);
            map.put("msg",model.getName()+"流程发布成功");
            log.info("流程发布成功，modelId={}",modelId);
            return ResponseEntity.ok(map);
        } catch (Exception e) {
            map.put("success", false);
            map.put("msg",model.getName()+"流程发布失败");
            log.error("modelId={}，流程发布失败，e={}",modelId,e);
        }
        return ResponseEntity.ok(map);
    }

}
