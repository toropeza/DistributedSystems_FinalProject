package Paxos;

import DataModel.DataServerList;
import DataModel.ServerInfo;
import Paxos.PaxosResponseModels.AcceptResponse;
import Paxos.PaxosResponseModels.CommittedResponse;
import Paxos.PaxosResponseModels.PrepareResponse;
import Paxos.PaxosResponseModels.ReadResponse;
import com.google.gson.Gson;
import util.HTTPHelper;

import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

/**
 * Paxos Manager is the main class for implementing the Paxos algorithm
 */
public class PaxosManager {

  static final Logger logger = Logger.getLogger(PaxosManager.class.getName());

  private final int REQUEST_TIMEOUT = 5000;

  private HTTPHelper httpHelper;
  private Gson gson;

  private DataServerList dataServerList;
  private int roundNumber;
  private Object value;
  private int currentRoundNumber = 0;
  private int killround = 0;
  private boolean accepted = false;
  private Object valueToPropose;

  public PaxosManager(DataServerList dataServerList, int killround, int startSequenceNum){
    if (killround > 0){
      logger.info("Starting Paxos Manager, will die on round " + killround);
    }
    this.dataServerList = dataServerList;
    httpHelper = new HTTPHelper();
    gson = new Gson();
    value = "empty";
    roundNumber = startSequenceNum;
    this.killround = killround;
  }

  public Object sendReadRequest(){
    int highestVersion = roundNumber;
    Object highestVersionData = value;
    List<ServerInfo> dataServers = dataServerList.getDataServerInfo();
    for (ServerInfo dataServer: dataServers){
      String readRequest = createReadRequest(dataServer);
      try {
        String response = httpHelper.performHttpGetWithTimeout(readRequest, REQUEST_TIMEOUT);
        ReadResponse readResponse = gson.fromJson(response, ReadResponse.class);
        if (readResponse.getSequenceNum() > highestVersion){
          highestVersion = readResponse.getSequenceNum();
          highestVersionData = readResponse.getValue();
        }
        //update lost data
        if (readResponse.getSequenceNum() > roundNumber){
          roundNumber = readResponse.getSequenceNum();
          value = highestVersionData = readResponse.getValue();
          logger.info("Found newer data on read request: updating");
        }
      }catch (SocketTimeoutException e){
        logger.warning("Unable to connect to Data Server: " + dataServer);
      }
    }
    logger.info("Highest versioned data found is " + highestVersion);
    return highestVersionData;
  }

  public boolean sendWriteRequest(Object value){
    boolean success = false;
    valueToPropose = value;
    int sequenceNum = roundNumber + 1;
    setCurrentRound(1);
    if (sendPrepareRequests(sequenceNum)){
      logger.info("Successfully received promise response from more than half the Data Servers");
      if (sendAcceptRequests(sequenceNum)){
        logger.info("Successfully received accept response from more than half the Data Servers");
        if (sendCommitRequests(sequenceNum)){
          logger.info("Successfully received committed response from more than half the Data Servers");
          setCurrentRound(5);
          success = true;
          roundNumber = sequenceNum;
          this.value = valueToPropose;
          logger.info("Setting value --> " + valueToPropose);
          currentRoundNumber = 0;
        }
      }
    }
    return success;
  }

  private boolean sendPrepareRequests(int sequenceNum){
    List<ServerInfo> dataServers = dataServerList.getDataServerInfo();
    int successfulResponses = 0;
    for (ServerInfo dataServer: dataServers){
      String prepareRequest = createPrepareRequest(dataServer, sequenceNum);
      try {
        String jsonResponse = httpHelper.performHttpGetWithTimeout(prepareRequest, REQUEST_TIMEOUT);
        PrepareResponse prepareResponse = gson.fromJson(jsonResponse, PrepareResponse.class);
        logger.info("Received response " + prepareResponse.promised() + " : " + prepareResponse.getPrevValue());
        if (prepareResponse.getPrevValue() != null){
          //finish Paxos round for previous value
          valueToPropose = prepareResponse.getPrevValue();
          sendAcceptRequests(sequenceNum);
          sendCommitRequests(sequenceNum);
          return false;
        }
        if (prepareResponse.promised()){
          successfulResponses++;
        }
      }catch (SocketTimeoutException e){
        logger.warning("Unable to connect to Data Server: " + dataServer);
      }
    }
    return successfulResponses >= (dataServers.size()/2) + 1;
  }

  private boolean sendAcceptRequests(int sequenceNum){
    List<ServerInfo> dataServers = dataServerList.getDataServerInfo();
    int successfulResponses = 0;
    for (int i=0; i < dataServers.size(); i++){
      if (i == 2){
        setCurrentRound(3);
      }
      ServerInfo dataServer = dataServers.get(i);
      String prepareRequest = createAcceptRequest(dataServer, valueToPropose , sequenceNum);
      try {
        String jsonResponse = httpHelper.performHttpGetWithTimeout(prepareRequest, REQUEST_TIMEOUT);
        AcceptResponse acceptResponse = gson.fromJson(jsonResponse, AcceptResponse.class);
        if (acceptResponse.accepted()){
          successfulResponses++;
        }
      }catch (SocketTimeoutException e){
        logger.warning("Unable to connect to Data Server: " + dataServer);
      }
    }
    return successfulResponses >= (dataServers.size()/2) + 1;
  }

  private boolean sendCommitRequests(int sequenceNum) {
    List<ServerInfo> dataServers = dataServerList.getDataServerInfo();
    int successfulResponses = 0;
    for (ServerInfo dataServer : dataServers) {
      String prepareRequest = createCommittedRequest(dataServer, valueToPropose, sequenceNum);
      try {
        String jsonResponse = httpHelper.performHttpGetWithTimeout(prepareRequest, REQUEST_TIMEOUT);
        CommittedResponse committedResponse = gson.fromJson(jsonResponse, CommittedResponse.class);
        if (committedResponse.isCommitted()) {
          successfulResponses++;
        }
      } catch (SocketTimeoutException e) {
        logger.warning("Unable to connect to Data Server: " + dataServer);
      }
    }
    return successfulResponses >= (dataServers.size() / 2) + 1;
  }

  public String parsePaxosRequest(String request){
    String response = httpHelper.HTTP404;
    String[] path = request.split("/");
    if (path.length >= 2) {
      String[] methodAndParams = path[2].split("\\?");
      String method = methodAndParams[0];
      switch (method){
        case PaxosAPI.PREPARE:
          setCurrentRound(2);
          response = parsePrepareRequest(methodAndParams[1]);
          break;
        case PaxosAPI.ACCEPT:
          setCurrentRound(4);
          response = parseAcceptRequest(methodAndParams[1]);
          break;
        case PaxosAPI.READ:
          response = parseReadRequest();
          break;
        case PaxosAPI.COMMITED:
          setCurrentRound(6);
          response = parseCommittedRequest(methodAndParams[1]);
          break;
        default:
          response = httpHelper.HTTP404;
          break;
      }
    }
    return httpHelper.buildHTTPResponse(response);
  }

  private String parseReadRequest(){
    ReadResponse readResponse = new ReadResponse();
    readResponse.setSequenceNum(roundNumber);
    readResponse.setValue(value);
    return gson.toJson(readResponse);
  }

  private String parsePrepareRequest(String params){
    PrepareResponse prepareResponse = new PrepareResponse();
    prepareResponse.setPromise(false);
    if (params != null){
      HashMap<String, String> urlParamsMap = httpHelper.parseURLParams(params);
      if (urlParamsMap != null && urlParamsMap.containsKey("sequenceNum")){
        int sequenceNum = Integer.valueOf(urlParamsMap.get("sequenceNum"));
        if (sequenceNum > roundNumber){
          this.roundNumber = sequenceNum;
          prepareResponse.setPromise(true);
          if (accepted){
            prepareResponse.setPrevValue(this.value);
          }
        }
      }
    }
    return gson.toJson(prepareResponse);
  }

  private String parseAcceptRequest(String params){
    AcceptResponse acceptResponse = new AcceptResponse();
    acceptResponse.setAccepted(false);
    if (params != null){
      HashMap<String, String> urlParamsMap = httpHelper.parseURLParams(params);
      if (urlParamsMap != null && urlParamsMap.containsKey("value") && urlParamsMap.containsKey("sequenceNum")){
        Object value = urlParamsMap.get("value");
        int sequenceNum = Integer.valueOf(urlParamsMap.get("sequenceNum"));
        if (sequenceNum >= roundNumber){
          this.roundNumber = sequenceNum;
          this.value = value;
          logger.info("Setting value -- > " + value);
          acceptResponse.setAccepted(true);
          accepted = true;
        }
      }
    }
    return gson.toJson(acceptResponse);
  }

  private String parseCommittedRequest(String params){
    CommittedResponse committedResponse = new CommittedResponse();
    committedResponse.setCommitted(false);
    if (params != null){
      HashMap<String, String> urlParamsMap = httpHelper.parseURLParams(params);
      if (urlParamsMap != null && urlParamsMap.containsKey("value") && urlParamsMap.containsKey("sequenceNum")){
        Object value = urlParamsMap.get("value");
        int sequenceNum = Integer.valueOf(urlParamsMap.get("sequenceNum"));
        if (sequenceNum >= roundNumber){
          this.roundNumber = sequenceNum;
          this.value = value;
          logger.info("Setting value -- > " + value);
          committedResponse.setCommitted(true);
          //reset accepted state
          accepted = false;
        }
      }
    }
    return gson.toJson(committedResponse);
  }

  private void setCurrentRound(int roundNumber){
    currentRoundNumber = roundNumber;
    if (currentRoundNumber == killround){
      logger.info("Killing Server for testing on round " + currentRoundNumber);
      System.exit(1);
    }
  }

  private String createPrepareRequest(ServerInfo dataServer, int sequenceNum){
    return buildBaseURL(dataServer) + PaxosAPI.PREPARE + "?sequenceNum=" + sequenceNum;
  }

  private String createAcceptRequest(ServerInfo dataServer, Object value, int sequenceNum){
    return buildBaseURL(dataServer) + PaxosAPI.ACCEPT + "?value=" + value + "&sequenceNum=" + sequenceNum;
  }

  private String createCommittedRequest(ServerInfo dataServer, Object value, int sequenceNum){
    return buildBaseURL(dataServer) + PaxosAPI.COMMITED+ "?value=" + value + "&sequenceNum=" + sequenceNum;
  }

  private String createReadRequest(ServerInfo dataServer){
    return buildBaseURL(dataServer) + PaxosAPI.READ;
  }

  private String buildBaseURL(ServerInfo dataServer){
    return "http://" + dataServer.getIp() + ":" + dataServer.getPort() +"/api/";
  }
}
