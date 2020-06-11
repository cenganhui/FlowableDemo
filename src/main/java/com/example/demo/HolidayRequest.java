package com.example.demo;

import org.flowable.engine.*;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class HolidayRequest {

    public static void main(String[] args) {

        // 通过ProcessEngineConfiguration实例创建ProcessEngine，该实例可以配置和调整流程引擎的设置
        // 通常用XML来创建，但也可以用编程方式创建
        // 配置JDBC连接
        ProcessEngineConfiguration cfg = new StandaloneProcessEngineConfiguration()
                .setJdbcUrl("jdbc:mysql://127.0.0.1:3306/flowable_test?autoReconnect=true&useUnicode=true&characterEncoding=utf-8&nullCatalogMeansCurrent=true&serverTimezone=GMT%2B8")
                .setJdbcUsername("root")
                .setJdbcPassword("nishi213")
                .setJdbcDriver("com.mysql.cj.jdbc.Driver")
                .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE); // 数据库架构不存在时创建数据库架构
        // 实例化ProcessEngine，这是一个线程安全的对象，通常在应用程序中只需实例化一次。
        ProcessEngine processEngine = cfg.buildProcessEngine();

        // 通过ProcessEngine对象获取到RepositoryService，将流程定义部署到Flowable引擎
        RepositoryService repositoryService = processEngine.getRepositoryService();
        // 通过传入XML文件并调用deploy()方法创建一个Deployment
        Deployment deployment = repositoryService.createDeployment()
                .addClasspathResource("holiday-request.bpmn20.xml")
                .deploy();

        // 通过AIP查询流程定义，以验证引擎是否知道流程定义（并了解有关API的知识）
        // 这是通过RepositoryService创建一个新的ProcessDefinitionQuery对象来完成的。
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .deploymentId(deployment.getId())
                .singleResult();
        System.out.println("Found process definition : " + processDefinition.getName());

        /*  启动流程实例  */
        // 录入一些简单的信息
        Scanner scanner = new Scanner(System.in);
        System.out.println("请问你是？");
        String employee = scanner.nextLine();
        System.out.println("那你想请几天假啊？");
        Integer nrOfHolidays = Integer.valueOf(scanner.nextLine());
        System.out.println("你为啥要请假啊？");
        String description = scanner.nextLine();

        // 通过RuntimeService启动流程实例
        RuntimeService runtimeService = processEngine.getRuntimeService();
        // 将数据以Map形式传递
        Map<String, Object> variables = new HashMap<>();
        variables.put("employee", employee);
        variables.put("nrOfHolidays", nrOfHolidays);
        variables.put("description", description);
        // key与holiday-request.bpmn20.xml中process的id匹配
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("holidayRequest", variables);

        /*  查询与完成任务 */
        // 通过ProcessEngine来获取TaskService
        TaskService taskService = processEngine.getTaskService();
        // 通过TaskService创建一个任务list
        List<Task> tasks = taskService.createTaskQuery().taskCandidateGroup("managers").list();
        System.out.println("你有 " + tasks.size() + " 任务：");
        for(int i = 0; i < tasks.size(); i++){
            System.out.println((i + 1) + ")" + tasks.get(i).getName());
        }

        // 选择一个要完成的任务
        System.out.println("***********管理人***********");
        System.out.println("你想要的完成哪一个任务？");
        int taskIndex = Integer.valueOf(scanner.nextLine());
        Task task = tasks.get(taskIndex - 1);
        Map<String, Object> processVariables = taskService.getVariables(task.getId());
        System.out.println(processVariables.get("employee") + " 想要请假 " + processVariables.get("nrOfHolidays") + " 天，你给不给嘛？(y or n)");

        // 通过输入y来判断允许或者拒绝请求
        // approved与holiday-request.bpmn20.xml中的网关匹配，决定路由到允许还是拒绝
        boolean approved = scanner.nextLine().toLowerCase().equals("y");
        variables = new HashMap<>();
        variables.put("approved", approved);
        // 完成任务
        taskService.complete(task.getId(), variables);

        // 历史数据处理，可以通过ProcessEngine来获取HistoryService，查询流程实例的持续时间等
        HistoryService historyService = processEngine.getHistoryService();
        List<HistoricActivityInstance> activities = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .finished()
                .orderByHistoricActivityInstanceEndTime().asc() // 按结束时间排序，意味着按执行顺序进行排序
                .list();

        for (HistoricActivityInstance activity : activities){
            System.out.println(activity.getActivityId() + " took " + activity.getDurationInMillis() + " ms");
        }

    }

}
