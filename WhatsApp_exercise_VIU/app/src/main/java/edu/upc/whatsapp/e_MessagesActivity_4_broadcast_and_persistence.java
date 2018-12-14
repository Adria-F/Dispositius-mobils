package edu.upc.whatsapp;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import java.util.Date;
import java.util.List;

import edu.upc.whatsapp.comms.RPC;
import edu.upc.whatsapp.adapter.MyAdapter_messages;
import entity.Message;
import entity.UserInfo;

import static edu.upc.whatsapp.comms.Comms.gson;

public class e_MessagesActivity_4_broadcast_and_persistence extends Activity {

  _GlobalState globalState;
  ProgressDialog progressDialog;
  private ListView conversation;
  private MyAdapter_messages adapter;
  private EditText input_text;
  private Button button;
  private InputMethodManager inMgr;
  private boolean enlarged = false, shrunk = true;

  private BroadcastReceiver broadcastReceiver;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.e_messages);
    globalState = (_GlobalState) getApplication();

    //para cuando venimos de la notificacion de status-bar:
    Intent intent = getIntent();
    if(intent.getExtras()!=null && intent.getExtras().get("message")!=null){

      //...
      String json = (String)intent.getExtras().get("message");
      Message message = gson.fromJson(json, Message.class);
      globalState.user_to_talk_to = message.getUserSender();
      globalState.save_new_message(message);
    }

    TextView title = (TextView) findViewById(R.id.title);
    title.setText("Talking with: " + globalState.user_to_talk_to.getName());
    setup_input_text();

    broadcastReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context arg0, Intent arg1) {

        //...
        String json = (String)arg1.getExtras().get("message");
        Message message = gson.fromJson(json, Message.class);
        if (globalState.user_to_talk_to.getId() == message.getUserSender().getId()) {
          adapter.addMessage(message);
          globalState.save_new_message(message);
          adapter.notifyDataSetChanged();
          conversation.post(new Runnable() {
            @Override
            public void run() {
              conversation.setSelection(conversation.getCount() - 1);
            }
          });
        }
        else
        {
          toastShow(message.getUserSender().getName()+": "+message.getContent(), -330);
        }
      }
    };
    IntentFilter intentFilter = new IntentFilter("edu.upc.whatsapp.newMessage");
    registerReceiver(broadcastReceiver, intentFilter);

    if (globalState.isThere_messages())
    {
        List<Message> messages = globalState.load_messages();
        adapter = new MyAdapter_messages(e_MessagesActivity_4_broadcast_and_persistence.this, messages, globalState.my_user);
        conversation = (ListView)findViewById(R.id.conversation);
        conversation.setAdapter(adapter);
        conversation.post(new Runnable() {
            @Override
            public void run() {
                conversation.setSelection(conversation.getCount()-1);
            }
        });
        toastShow(messages.size()+" messages loaded",200);

      new fetchNewMessages_Task().execute(globalState.my_user.getId(), globalState.user_to_talk_to.getId());
    }
    else
    {
        new fetchAllMessages_Task().execute(globalState.my_user.getId(), globalState.user_to_talk_to.getId());
    }
  }

  @Override
  protected void onResume() {
    super.onResume();

    //...
    super.onResume();
    globalState.MessagesActivity_visible = true;

  }

  @Override
  protected void onPause() {
    super.onPause();

    //...
    super.onPause();
    globalState.MessagesActivity_visible = false;

  }

  @Override
  protected void onDestroy(){
    super.onDestroy();
    if (broadcastReceiver != null) {
      unregisterReceiver(broadcastReceiver);
    }
  }

  private class fetchAllMessages_Task extends AsyncTask<Integer, Void, List<Message>> {

    @Override
    protected void onPreExecute() {
      progressDialog = ProgressDialog.show(e_MessagesActivity_4_broadcast_and_persistence.this,
              "MessagesActivity", "downloading messages...");
    }

    @Override
    protected List<Message> doInBackground(Integer... userIds) {

      //...
      return RPC.retrieveMessages(userIds[0], userIds[1]);

    }

    @Override
    protected void onPostExecute(List<Message> all_messages) {
      progressDialog.dismiss();
      if (all_messages == null) {
        toastShow("There's been an error downloading the messages",200);
      } else {
        toastShow(all_messages.size()+" messages downloaded",200);

        //...
          globalState.save_new_messages(all_messages);
        adapter = new MyAdapter_messages(e_MessagesActivity_4_broadcast_and_persistence.this, all_messages, globalState.my_user);
        conversation = (ListView)findViewById(R.id.conversation);
        conversation.setAdapter(adapter);
        conversation.post(new Runnable() {
          @Override
          public void run() {
            conversation.setSelection(conversation.getCount()-1);
          }
        });
      }
    }
  }

  private class fetchNewMessages_Task extends AsyncTask<Integer, Void, List<Message>> {

    @Override
    protected List<Message> doInBackground(Integer... userIds) {

      //...
      if (adapter.isEmpty())
        return RPC.retrieveMessages(userIds[0], userIds[1]);
      else
        return RPC.retrieveNewMessages(userIds[0], userIds[1], adapter.getLastMessage());
    }

    @Override
    protected void onPostExecute(List<Message> new_messages) {
      if (new_messages == null) {
        toastShow("There's been an error downloading new messages", 200);
      } else {
        toastShow(new_messages.size()+" new message/s downloaded", 200);

        //...

        if (new_messages.size() > 0 && new_messages.get(0).getId() == -1)
          new fetchAllMessages_Task().execute(globalState.my_user.getId(), globalState.user_to_talk_to.getId());
        else if (new_messages.size() > 0) {
          globalState.save_new_messages(new_messages);
          adapter.addMessages(new_messages);
          adapter.notifyDataSetChanged();
          conversation.post(new Runnable() {
            @Override
            public void run() {
              conversation.setSelection(conversation.getCount() - 1);
            }
          });
        }
      }
    }
  }

  public void sendText(final View view) {

    //...

    Message msg = new Message();
    msg.setContent(input_text.getText().toString());
    msg.setUserSender(globalState.my_user);
    msg.setUserReceiver(globalState.user_to_talk_to);
    msg.setDate(new Date());

    new SendMessage_Task().execute(msg);

    input_text.setText("");

    //to hide the soft keyboard after sending the message:
    InputMethodManager inMgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
    inMgr.hideSoftInputFromWindow(input_text.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
  }
  private class SendMessage_Task extends AsyncTask<Message, Void, Boolean> {

    @Override
    protected void onPreExecute() {
      toastShow("sending message",200);
    }

    @Override
    protected Boolean doInBackground(Message... messages) {

      //...
      return RPC.postMessage(messages[0]);
    }

    @Override
    protected void onPostExecute(Boolean resultOk) {
      if (resultOk) {
        toastShow("message sent", 200);

        //...

        new fetchNewMessages_Task().execute(globalState.my_user.getId(), globalState.user_to_talk_to.getId());

      } else {
        toastShow("There's been an error sending the message", 200);
      }
    }
  }

  private void setup_input_text(){

    input_text = (EditText) findViewById(R.id.input);
    button = (Button) findViewById(R.id.mybutton);
    button.setEnabled(false);

    //to be notified when the content of the input_text is modified:
    input_text.addTextChangedListener(new TextWatcher() {

      public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
      }

      public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
      }

      public void afterTextChanged(Editable arg0) {
        if (arg0.toString().equals("")) {
          button.setEnabled(false);
        } else {
          button.setEnabled(true);
        }
      }
    });
    //to program the send soft key of the soft keyboard:
    input_text.setOnEditorActionListener(new OnEditorActionListener() {
      @Override
      public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        boolean handled = false;
        if (actionId == EditorInfo.IME_ACTION_SEND) {
          sendText(null);
          handled = true;
        }
        return handled;
      }
    });
    //to detect a change on the height of the window on the screen:
    input_text.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
      @Override
      public void onGlobalLayout() {
        int screenHeight = input_text.getRootView().getHeight();
        Rect r = new Rect();
        input_text.getWindowVisibleDisplayFrame(r);
        int visibleHeight = r.bottom - r.top;
        int heightDifference = screenHeight - visibleHeight;
        if (heightDifference > 50 && !enlarged) {
          LayoutParams layoutparams = input_text.getLayoutParams();
          layoutparams.height = layoutparams.height * 2;
          input_text.setLayoutParams(layoutparams);
          enlarged = true;
          shrunk = false;
          conversation.post(new Runnable() {
            @Override
            public void run() {
              conversation.setSelection(conversation.getCount() - 1);
            }
          });
        }
        if (heightDifference < 50 && !shrunk) {
          LayoutParams layoutparams = input_text.getLayoutParams();
          layoutparams.height = layoutparams.height / 2;
          input_text.setLayoutParams(layoutparams);
          shrunk = true;
          enlarged = false;
        }
      }
    });
  }

  private void toastShow(String text, int yOffset) {
    Toast toast = Toast.makeText(this, text, Toast.LENGTH_LONG);
    toast.setGravity(0, 0, yOffset);
    toast.show();
  }

}
