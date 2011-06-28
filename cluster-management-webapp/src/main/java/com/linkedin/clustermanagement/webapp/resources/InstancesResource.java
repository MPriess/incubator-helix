package com.linkedin.clustermanagement.webapp.resources;

import java.util.List;
import java.util.Map;

import org.restlet.Context;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.Resource;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;

import com.linkedin.clustermanager.core.ClusterDataAccessor;
import com.linkedin.clustermanager.core.ClusterDataAccessor.ClusterPropertyType;
import com.linkedin.clustermanager.core.ClusterDataAccessor.InstanceConfigProperty;
import com.linkedin.clustermanager.model.ZNRecord;
import com.linkedin.clustermanager.tools.ClusterSetup;
import com.linkedin.clustermanager.util.ZNRecordUtil;

public class InstancesResource extends Resource
{
  public InstancesResource(Context context,
      Request request,
      Response response) 
  {
    super(context, request, response);
    getVariants().add(new Variant(MediaType.TEXT_PLAIN));
    getVariants().add(new Variant(MediaType.APPLICATION_JSON));
  }

  public boolean allowGet()
  {
    return true;
  }
  
  public boolean allowPost()
  {
    return true;
  }
  
  public boolean allowPut()
  {
    return false;
  }
  
  public boolean allowDelete()
  {
    return false;
  }
  
  public Representation represent(Variant variant)
  {
    StringRepresentation presentation = null;
    try
    {
      String zkServer = (String)getContext().getAttributes().get("zkServer");
      String clusterName = (String)getRequest().getAttributes().get("clusterName");
      presentation = getInstancesRepresentation(zkServer, clusterName);
    }
    
    catch(Exception e)
    {
      getResponse().setEntity("ERROR " + e.getMessage(),
          MediaType.TEXT_PLAIN);
      getResponse().setStatus(Status.SUCCESS_OK);
    }  
    return presentation;
  }
  
  StringRepresentation getInstancesRepresentation(String zkServerAddress, String clusterName)
  {
    ClusterSetup setupTool = new ClusterSetup(zkServerAddress);
    List<String> instances = setupTool.getClusterManagementTool().getNodeNamesInCluster(clusterName);
    String message = "Instances in cluster "+ clusterName + "\nTotal "+ instances.size() + " instances:\n";
    
    ClusterDataAccessor accessor = ClusterRepresentationUtil.getClusterDataAccessor(zkServerAddress,  clusterName);
    List<ZNRecord> liveInstances = accessor.getClusterPropertyList(ClusterPropertyType.LIVEINSTANCES);
    List<ZNRecord> instanceConfigs = accessor.getClusterPropertyList(ClusterPropertyType.CONFIGS);
    
    Map<String, ZNRecord> liveInstanceMap = ZNRecordUtil.convertListToMap(liveInstances);
    Map<String, ZNRecord> configsMap = ZNRecordUtil.convertListToMap(instanceConfigs);
    
    for (String instanceName : instances)
    {
      boolean isAlive = liveInstanceMap.containsKey(instanceName);
      String enabled = configsMap.get(instanceName).getSimpleField(InstanceConfigProperty.ENABLED.toString());
      
      message = message + "{ Instance : "+ instanceName + " alive: "+ isAlive +" enabled: "+ enabled +"}\n";
    }
    StringRepresentation representation = new StringRepresentation(message, MediaType.APPLICATION_JSON);
    
    return representation;
  }
  
  public void acceptRepresentation(Representation entity)
  {
    try
    {
      String zkServer = (String)getContext().getAttributes().get("zkServer");
      String clusterName = (String)getRequest().getAttributes().get("clusterName");
      
      Form form = new Form(entity);
      String instanceName = form.getFirstValue("instanceName");
      String instanceNames = form.getFirstValue("instanceNames");
      ClusterSetup setupTool = new ClusterSetup(zkServer);
      if(instanceName != null)
      {
        setupTool.addNodeToCluster(clusterName,instanceName);
      }
      else if(instanceNames != null)
      {
        setupTool.addNodesToCluster(clusterName, instanceNames.split(";"));
      }
      
      // add cluster
      getResponse().setEntity(getInstancesRepresentation(zkServer, clusterName));
      getResponse().setStatus(Status.SUCCESS_OK);
    }

    catch(Exception e)
    {
      getResponse().setEntity("ERROR " + e.getMessage(),
          MediaType.TEXT_PLAIN);
      getResponse().setStatus(Status.SUCCESS_OK);
    }  
  }
}