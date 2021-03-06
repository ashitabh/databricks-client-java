package com.level11data.databricks.cluster;


import com.level11data.databricks.client.ClustersClient;
import com.level11data.databricks.client.HttpException;
import com.level11data.databricks.client.entities.clusters.ClusterInfoDTO;

import java.util.*;

public abstract class AbstractBaseCluster {
    private Boolean _clusterInfoRequested = false;
    private ClusterInfoDTO _clusterInfoDTO;
    private ClustersClient _client;
    private String _clusterId;
    protected Boolean IsAutoScaling = false;

    public final Integer NumWorkers;
    public final AutoScale AutoScale;

    public final String Name;
    public final AwsAttributes AwsAttributes;
    public final Boolean ElasticDiskEnabled;
    public final Map<String, String> SparkConf;
    public final List<String> SshPublicKeys;
    public final Map<String, String> DefaultTags;
    public final Map<String, String> CustomTags;
    public final ClusterLogConf ClusterLogConf;
    public final Map<String, String> SparkEnvironmentVariables;

    //This signature is used by ClusterSpec
    protected AbstractBaseCluster(ClusterInfoDTO clusterInfoDTO) throws ClusterConfigException {
        //Validate that required fields are populated in the ClusterInfoDTO
        validateClusterInfo(clusterInfoDTO);

        _clusterInfoDTO = clusterInfoDTO;
        _clusterId = clusterInfoDTO.ClusterId;  //should this ALWAYS be null??

        Name = clusterInfoDTO.ClusterName; //could be null
        NumWorkers = clusterInfoDTO.NumWorkers;  //could be null
        AutoScale = initAutoScale(clusterInfoDTO);  //could be null

        AwsAttributes = clusterInfoDTO.AwsAttributes == null ? null : new AwsAttributes(clusterInfoDTO.AwsAttributes);
        ElasticDiskEnabled = clusterInfoDTO.EnableElasticDisk;

        HashMap<String,String> sparkConfMap = new HashMap<>();
        if(clusterInfoDTO.SparkConf != null) {
            sparkConfMap.putAll(clusterInfoDTO.SparkConf);
        }
        SparkConf = Collections.unmodifiableMap(sparkConfMap);

        ArrayList<String> sshKeyList = new ArrayList<>();
        if(clusterInfoDTO.SshPublicKeys != null) {
            for (String sshPublicKey : clusterInfoDTO.SshPublicKeys) {
                sshKeyList.add(sshPublicKey);
            }
        }
        SshPublicKeys = Collections.unmodifiableList(sshKeyList);

        HashMap<String,String> defaultTagsMap = new HashMap<>();
        if(clusterInfoDTO.DefaultTags != null) {
            defaultTagsMap.putAll(clusterInfoDTO.DefaultTags);
        }
        DefaultTags = Collections.unmodifiableMap(defaultTagsMap);

        HashMap<String,String> customTagsMap = new HashMap<>();
        if(clusterInfoDTO.CustomTags != null) {
            customTagsMap.putAll(clusterInfoDTO.CustomTags);
        }
        CustomTags = Collections.unmodifiableMap(customTagsMap);

        ClusterLogConf = clusterInfoDTO.ClusterLogConf == null
                ? null : new ClusterLogConf(clusterInfoDTO.ClusterLogConf);

        HashMap<String,String> sparkEnvVarMap = new HashMap<>();
        if(clusterInfoDTO.SparkEnvironmentVariables != null) {
            sparkEnvVarMap.putAll(clusterInfoDTO.SparkEnvironmentVariables);
        }
        SparkEnvironmentVariables = Collections.unmodifiableMap(sparkEnvVarMap);
    }

    protected AbstractBaseCluster(ClustersClient client, ClusterInfoDTO clusterInfoDTO) throws ClusterConfigException {
        //Validate that required fields are populated in the ClusterInfoDTO
        validateClusterInfo(clusterInfoDTO);

        _client = client;
        _clusterInfoDTO = clusterInfoDTO;
        _clusterId = clusterInfoDTO.ClusterId;  //TODO should this ALWAYS be populated?

        //Set fields that do not change throughout the lifespan of a cluster configuration
        // these fields may not have been set in the DTO if object was instantiated from InteractiveClusterBuilder.create()
        // therefore they may need to be initialized with an API call to get the ClusterInfo
        Name = initClusterName();
        NumWorkers = clusterInfoDTO.NumWorkers;  //could be null
        AutoScale = initAutoScale(clusterInfoDTO);  //could be null

        AwsAttributes = initAwsAttributes();
        ElasticDiskEnabled = initElasticDiskEnabled();
        SparkConf = Collections.unmodifiableMap(initSparkConf());
        SshPublicKeys = Collections.unmodifiableList(initSshPublicKeys());
        DefaultTags = Collections.unmodifiableMap(initDefaultTags());
        CustomTags = Collections.unmodifiableMap(initCustomTags());
        SparkEnvironmentVariables = Collections.unmodifiableMap(initSparkEnvironmentVariables());
        ClusterLogConf = initLogConf();
    }

    private AutoScale initAutoScale(ClusterInfoDTO clusterInfoDTO) {
        if(clusterInfoDTO.AutoScale != null){
            IsAutoScaling = true;
            return new AutoScale(clusterInfoDTO.AutoScale);
        } else {
            return null;
        }
    }

    private void validateClusterInfo(ClusterInfoDTO info) throws ClusterConfigException {
        if(_clusterId != null && info.ClusterId == null) {
            throw new ClusterConfigException("ClusterInfoDTO Must Have ClusterId");
        }

        if(info.NumWorkers == null && info.AutoScale == null)  {
            throw new ClusterConfigException("ClusterInfoDTO Must Have either NumWorkers OR AutoScaleDTO");
        }
    }

    protected ClusterInfoDTO getClusterInfo() throws ClusterConfigException {
        try {
            if(_client == null) {
                return _clusterInfoDTO;
            } else {
                if(!_clusterInfoRequested) {
                    _clusterInfoDTO = _client.getCluster(_clusterId);
                    _clusterInfoRequested = true;
                    return _clusterInfoDTO;
                } else {
                    return _clusterInfoDTO;
                }
            }
        } catch(HttpException e) {
            throw new ClusterConfigException(e);
        }

    }

    private String initClusterName() throws ClusterConfigException {
        return getClusterInfo().ClusterName;
    }

    private AwsAttributes initAwsAttributes() throws ClusterConfigException {
        if(getClusterInfo().AwsAttributes == null) {
            return null;
        } else {
            return new AwsAttributes(getClusterInfo().AwsAttributes);
        }
    }



    private Boolean initElasticDiskEnabled() throws ClusterConfigException {
        return getClusterInfo().EnableElasticDisk;
    }

    private Map<String, String> initSparkConf() throws ClusterConfigException {
        if(getClusterInfo().SparkConf == null) {
            return new HashMap<String,String>();
        } else {
            return getClusterInfo().SparkConf;
        }
    }

    private List<String> initSshPublicKeys() throws ClusterConfigException {
        if(getClusterInfo().SshPublicKeys == null) {
            return new ArrayList<>();
        } else {
            List<String> sshPublicKeysList = new ArrayList<>();
            for (String ssh : getClusterInfo().SshPublicKeys) {
                sshPublicKeysList.add(ssh);
            }
            return sshPublicKeysList;
        }
    }

    private Map<String, String> initCustomTags() throws ClusterConfigException {
        if(getClusterInfo().CustomTags == null) {
            return new HashMap<>();
        } else {
            return getClusterInfo().CustomTags;
        }
    }

    private ClusterLogConf initLogConf() throws ClusterConfigException {
        if(getClusterInfo().ClusterLogConf == null) {
            return null;
        } else {
            return new ClusterLogConf(getClusterInfo().ClusterLogConf);
        }
    }

    private Map<String, String> initSparkEnvironmentVariables() throws ClusterConfigException {
        if(getClusterInfo().SparkEnvironmentVariables == null) {
            return new HashMap<>();
        } else {
            return getClusterInfo().SparkEnvironmentVariables;
        }
    }

    private Map<String, String> initDefaultTags() throws ClusterConfigException {
        if(getClusterInfo().DefaultTags == null) {
            return new HashMap<>();
        } else {
            return getClusterInfo().DefaultTags;
        }
    }



}
