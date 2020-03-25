package com.example.demo.config;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.*;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Zookeeper配置雪花算法
 */
@Component
public class ZookeeperCustor {

    private final static Logger log = LoggerFactory.getLogger(ZookeeperCustor.class);

    @Value("${zookeeper-server}")
    public String zookeeperServer;

    @Autowired
    private SnowflakeIdWorker snowflakeIdWorker;

    private CuratorFramework client = null;

    private Integer workid;

    private final String PATH = "/Idgenerator";

    private String currentNode;

    public void init() {
        if (client != null) {
            return;
        }
        //创建重试策略
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 5);
        //创建zookeeper客户端
        client = CuratorFrameworkFactory.builder().connectString(zookeeperServer)
                .sessionTimeoutMs(10000)
                .retryPolicy(retryPolicy)
                .namespace("service")
                .build();
        client.start();
        try {
            initWorkId();
            initListener();
        } catch (Exception e) {
            e.printStackTrace();
            log.error("初始化创建节点失败");
        }
        log.info("zookeeper初始化成功");
    }

    /**
     * 初始化创建节点
     */
    private void initWorkId() throws Exception {
        currentNode = client.create().creatingParentsIfNeeded()
                .withMode(CreateMode.EPHEMERAL_SEQUENTIAL).forPath(PATH + "/Idgenerator_");
        log.info("当前节点node{}", currentNode);
        orderNodes();
    }

    /**
     * 节点变更监听器
     *
     * @throws Exception
     */
    private void initListener() throws Exception {
        //创建子节点watcher监听
        TreeCache cache = new TreeCache(client, PATH);
        cache.start();
        cache.getListenable().addListener((curatorFramework, treeCacheEvent) -> {
            ChildData data = treeCacheEvent.getData();
            switch (treeCacheEvent.getType()) {
                case NODE_ADDED:
                    log.info("[TreeCache]节点增加, path={}, data={}", data.getPath(), data.getData());
                    break;
                case NODE_UPDATED:
                    log.info("[TreeCache]节点更新, path={}, data={}", data.getPath(), data.getData());
                    break;
                case NODE_REMOVED:
                    log.info("[TreeCache]节点删除, path={}, data={}", data.getPath(), data.getData());
                    orderNodes();
                    break;
                default:
                    break;
            }
        });
    }

    /**
     * 分配机器id
     *
     * @throws Exception
     */
    private void orderNodes() throws Exception {
        List<String> nodes = client.getChildren().forPath(PATH);
        //节点升序
        nodes.sort(String::compareTo);
        workid = null;
        for (int i = 0; i < nodes.size(); i++) {
            if (currentNode.indexOf(nodes.get(i)) != -1) {
                workid = i;
            }
        }
        if (workid == null) {
            throw new RuntimeException("获取机器id失败！");
        }
        log.info("当前分配机器id为{}", workid);
        snowflakeIdWorker.setWorkerId(workid);
    }

}
