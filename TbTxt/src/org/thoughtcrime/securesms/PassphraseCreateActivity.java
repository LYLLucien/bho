/** 
 * Copyright (C) 2011 Whisper Systems
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.thoughtcrime.securesms;

import org.ironrabbit.tbtxt.R;
import org.thoughtcrime.securesms.crypto.IdentityKeyUtil;
import org.thoughtcrime.securesms.crypto.MasterSecret;
import org.thoughtcrime.securesms.crypto.MasterSecretUtil;
import org.thoughtcrime.securesms.lang.BhoButton;
import org.thoughtcrime.securesms.lang.BhoEditText;
import org.thoughtcrime.securesms.lang.BhoTextView;
import org.thoughtcrime.securesms.util.MemoryCleaner;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.RemoteViews;
import android.widget.Toast;

/**
 * Activity for creating a user's local encryption passphrase.
 * 
 * @author Moxie Marlinspike
 */
public class PassphraseCreateActivity extends PassphraseActivity {

  private BhoEditText	   passphraseEdit;
  private BhoEditText	   passphraseRepeatEdit;
  private BhoButton       okButton;
  private BhoButton       cancelButton;
	
  public PassphraseCreateActivity() {	}

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
   getWindow().requestFeature(Window.FEATURE_NO_TITLE);
    setContentView(R.layout.create_passphrase_activity);
		
    initializeResources();
  }
	
  private void initializeResources() {
	  
    this.passphraseEdit       = (BhoEditText) findViewById(R.id.passphrase_edit);
    this.passphraseRepeatEdit = (BhoEditText) findViewById(R.id.passphrase_edit_repeat);
    this.okButton             = (BhoButton) findViewById(R.id.ok_button);
    this.cancelButton         = (BhoButton) findViewById(R.id.cancel_button);
		
    this.okButton.setOnClickListener(new OkButtonClickListener());
    this.cancelButton.setOnClickListener(new CancelButtonClickListener());
  }
	
  private void verifyAndSavePassphrases() {
    String passphrase       = this.passphraseEdit.getText().toString();
    String passphraseRepeat = this.passphraseRepeatEdit.getText().toString();
		
    if (!passphrase.equals(passphraseRepeat)) {
      Toast.makeText(getApplicationContext(), R.string.passphrases_don_t_match_, Toast.LENGTH_SHORT).show();
      this.passphraseEdit.setText("");
      this.passphraseRepeatEdit.setText("");
    } else {
      MasterSecret masterSecret = MasterSecretUtil.generateMasterSecret(this, passphrase);
      MemoryCleaner.clean(passphrase); // We do this, but the edit boxes are basically impossible to clean up.
      MemoryCleaner.clean(passphraseRepeat);
      new AsymmetricSecretGenerator(masterSecret).generate();
    }
  }
	
  private class AsymmetricSecretGenerator extends Handler implements Runnable {
    private ProgressDialog progressDialog;
    private MasterSecret masterSecret;
		
    public AsymmetricSecretGenerator(MasterSecret masterSecret) {
      this.masterSecret = masterSecret;
    }
		
    public void run() {
      MasterSecretUtil.generateAsymmetricMasterSecret(PassphraseCreateActivity.this, masterSecret);
      IdentityKeyUtil.generateIdentityKeys(PassphraseCreateActivity.this, masterSecret);
      this.obtainMessage().sendToTarget();
    }
		
    public void generate() {
      
      progressDialog = new ProgressDialog(PassphraseCreateActivity.this);
      progressDialog.setCancelable(false);
      progressDialog.setIndeterminate(true);
      progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
      
      progressDialog.show();
      
      
      View view = LayoutInflater.from(PassphraseCreateActivity.this).inflate(R.layout.custom_notifications, null);  
      
  //    contentView.setImageViewResource(R.id.image, R.drawable.ic_launcher);
   //   contentView.setTextViewText(R.id.title, getString(R.string.generating_keypair));
    //  contentView.setTextViewText(R.id.text, getString(R.string.generating_a_local_encryption_keypair_));
      ((BhoTextView)view.findViewById(R.id.title)).setText(getString(R.string.generating_keypair));
      ((BhoTextView)view.findViewById(R.id.text)).setText(getString(R.string.generating_a_local_encryption_keypair_));
      
      progressDialog.setContentView(view);

      
     // progressDialog.setTitle(R.string.generating_keypair);
     // progressDialog.setMessage(getString(R.string.generating_a_local_encryption_keypair_));
      
      
      new Thread(this).start();			
    }
		
    @Override
    public void handleMessage(Message message) {
      progressDialog.dismiss();
      setMasterSecret(masterSecret);
    }
  }
	
  private class CancelButtonClickListener implements OnClickListener {
    public void onClick(View v) {	
      finish();
    }
  }
	
  private class OkButtonClickListener implements OnClickListener {
    public void onClick(View v) {
      verifyAndSavePassphrases();
    }
  }

  @Override
  protected void cleanup() {
    this.passphraseEdit       = null;
    this.passphraseRepeatEdit = null;
    System.gc();
  }
}
