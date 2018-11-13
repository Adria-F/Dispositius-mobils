<%@page import="webSocketService.WebSocketServer"%>
<%@page import="com.google.gson.GsonBuilder"%>
<%@page import="util.DateSerializerDeserializer"%>
<%@page import="java.util.Date"%>
<%@page import="entity.Message"%>
<%@page import="entity.UserInfo"%>
<%@page import="entity.User"%>
<%@page import="com.google.gson.Gson"%>
<%@page import="java.io.InputStreamReader"%>
<%@page import="java.io.BufferedReader"%>
<%@page pageEncoding="UTF-8"%>

<jsp:useBean id="global" scope="application" class="util.Global" />

<%

  BufferedReader rd = new BufferedReader(new InputStreamReader(request.getInputStream(),"UTF-8"));
  Gson gson = new GsonBuilder().registerTypeAdapter(Date.class, new DateSerializerDeserializer()).create();
  Message message = gson.fromJson(rd, Message.class);

  System.out.println("deleting message: "+message.getContent()+ " at date:"+message.getDate());
  
  try{
    User sender   = global.users.get(message.getUserSender().getId());
    User receiver = global.users.get(message.getUserReceiver().getId());
    if(sender==null || receiver==null || message.getContent().isEmpty() ){
      out.clear();
      return;
    }
  }
  catch(Exception e){
    e.printStackTrace();
    out.clear();
    return;
  }
  
  global.messages.remove(message);
  global.usersToNotify.add(message.getUserReceiver());
  
  out.clear();
%>