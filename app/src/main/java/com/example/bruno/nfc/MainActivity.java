  package com.example.bruno.nfc;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcAdapter.CreateNdefMessageCallback;
import android.nfc.NfcEvent;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.nio.charset.Charset;
import java.util.Locale;


  public class MainActivity extends AppCompatActivity {
      NfcAdapter mNfcAdapter;
      private TextView messageText;
      private String payload="";

      private EditText inputEditText;

      byte statusByte;

      @Override
      public void onCreate(Bundle savedInstanceState) {
          super.onCreate(savedInstanceState);
          setContentView(R.layout.activity_main);
          messageText = (TextView) this.findViewById(R.id.messageText);
          mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
          if (mNfcAdapter == null) {
              messageText.setText("NFC apdater  is not available");
              finish();
              return;
          }
          messageText.setText("Write some text to share");
      }

      public void onClickHandler(View view) {

          if(view.getId() == R.id.shareButton){

              inputEditText = (EditText)this.findViewById(R.id.inputEditText);
              String inputText = inputEditText.getText().toString();
              NdefMessage message=create_RTD_TEXT_NdefMessage(inputText);
              mNfcAdapter.setNdefPushMessage(message, this);
              Toast.makeText(this, "Touch another mobile to share the message", Toast.LENGTH_SHORT).show();
          }
      }

      @Override
      public void onResume() {
          super.onResume();
          if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
              processIntent(getIntent());
          }
      }

      @Override
      public void onNewIntent(Intent intent) {
          setIntent(intent);
      }

      void processIntent(Intent intent) {


          NdefMessage[] messages = getNdefMessages(getIntent());
          for(int i=0;i<messages.length;i++){
              for(int j=0;j<messages[0].getRecords().length;j++){
                  NdefRecord record = messages[i].getRecords()[j];
                  statusByte=record.getPayload()[0];
                  int languageCodeLength= statusByte & 0x3F; //mask value in order to find language code length
                  int isUTF8=statusByte-languageCodeLength;
                  if(isUTF8==0x00){
                      payload=new String(record.getPayload(),1+languageCodeLength,record.getPayload().length-1-languageCodeLength,Charset.forName("UTF-8"));
                  }
                  else if (isUTF8==-0x80){
                      payload=new String(record.getPayload(),1+languageCodeLength,record.getPayload().length-1-languageCodeLength,Charset.forName("UTF-16"));
                  }
                  messageText.setText("Text received: "+ payload);
              }
          }
      }

      NdefMessage create_RTD_TEXT_NdefMessage(String inputText){

          Locale locale= new Locale("en","US");
          byte[] langBytes = locale.getLanguage().getBytes(Charset.forName("US-ASCII"));

          boolean encodeInUtf8=false;
          Charset utfEncoding = encodeInUtf8 ? Charset.forName("UTF-8") : Charset.forName("UTF-16");
          int utfBit = encodeInUtf8 ? 0 : (1 << 7);
          byte status = (byte) (utfBit + langBytes.length);

          byte[] textBytes = inputText.getBytes(utfEncoding);

          byte[] data = new byte[1 + langBytes.length + textBytes.length];
          data[0] = (byte) status;
          System.arraycopy(langBytes, 0, data, 1, langBytes.length);
          System.arraycopy(textBytes, 0, data, 1 + langBytes.length, textBytes.length);

          NdefRecord textRecord = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,
                  NdefRecord.RTD_TEXT, new byte[0], data);
          NdefMessage message= new NdefMessage(new NdefRecord[] { textRecord});
          return message;

      }
      NdefMessage[] getNdefMessages(Intent intent) {

          NdefMessage[] msgs = null;
          if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
              Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
              if (rawMsgs != null) {
                  msgs = new NdefMessage[rawMsgs.length];
                  for (int i = 0; i < rawMsgs.length; i++) {
                      msgs[i] = (NdefMessage) rawMsgs[i];
                  }
              } else {
                  byte[] empty = new byte[]{};
                  NdefRecord record = new NdefRecord(NdefRecord.TNF_UNKNOWN, empty, empty, empty);
                  NdefMessage msg = new NdefMessage(new NdefRecord[]{
                          record
                  });
                  msgs = new NdefMessage[]{
                          msg
                  };
              }
          } else {
              Log.d("Peer to Peer 2", "Unknown intent.");
              finish();
          }

          return msgs;
      }}