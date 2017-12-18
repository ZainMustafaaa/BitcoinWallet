package bitcoin.wallet.com.bitcoinwallet;

import android.graphics.Color;
import android.opengl.Visibility;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.util.concurrent.Service;
import com.lambdaworks.jni.Platform;

import org.bitcoinj.core.BlockChain;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.core.StoredBlock;
import org.bitcoinj.core.listeners.DownloadProgressTracker;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.TestNet2Params;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.store.BlockStore;
import org.bitcoinj.store.BlockStoreException;
import org.bitcoinj.store.MySQLFullPrunedBlockStore;
import org.bitcoinj.utils.Threading;
import org.bitcoinj.wallet.Protos;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.store.H2FullPrunedBlockStore;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.Executor;

public class MainActivity extends AppCompatActivity {


    final static String APP_NAME = "Wallet_Bitcoin";
    static NetworkParameters params = TestNet3Params.get();
    static WalletAppKit bitcoin;

    TextView textView, currentReceiveAddress, ecKey, recieveFreshAddress, tvSeeds;
    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = (TextView) findViewById(R.id.progressText);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        currentReceiveAddress = (TextView) findViewById(R.id.currentReceiveAddress);
        tvSeeds = (TextView) findViewById(R.id.tv_seed);
        ecKey = (TextView) findViewById(R.id.ecKey);
        recieveFreshAddress = (TextView) findViewById(R.id.freshAddress);

        bitcoin = new WalletAppKit(params, Environment.getExternalStorageDirectory(), APP_NAME + params.getPaymentProtocolId()) {
            @Override
            protected void onSetupCompleted() {
                bitcoin.wallet().allowSpendingUnconfirmedTransactions();
                bitcoin.useTor();
            }
        };

        bitcoin.setDownloadListener(new Progress())
                .setBlockingStartup(false)
                .setUserAgent(APP_NAME, "1.0");

        try {
            if (bitcoin.isChainFileLocked()) {
                Toast.makeText(this, "chain file locked", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        bitcoin.addListener(new Service.Listener() {
            @Override
            public void failed(Service.State from, Throwable failure) {
                textView.setText(failure.toString());
            }
        }, new Executor() {
            @Override
            public void execute(@NonNull Runnable command) {
                command.run();
            }
        });

        textView.setText(bitcoin.startAsync().toString());

        Wallet wallet = new Wallet(params);
        try {
            BlockChain chain = new BlockChain(params, wallet, new H2FullPrunedBlockStore(params, APP_NAME, 1000));
            PeerGroup peerGroup = new PeerGroup(params, chain);
            peerGroup.addWallet(wallet);
            peerGroup.startAsync();


        } catch (BlockStoreException e) {
            e.printStackTrace();
            currentReceiveAddress.setText(e.getMessage());
        }

        currentReceiveAddress.setText("Address: " + wallet.currentReceiveAddress().toString());
        ecKey.setText("ECKEY: " + wallet.currentReceiveKey());
        recieveFreshAddress.setText("Recieve F-Address: " + wallet.freshReceiveAddress().toString());
        tvSeeds.setText(wallet.getKeyChainSeed().toString());

    }
    class Progress extends DownloadProgressTracker{
        @Override
        protected void progress(double pct, int blocksSoFar, Date date) {

            progressBar.setProgress(blocksSoFar);

        }

        @Override
        protected void startDownload(int blocks) {
            progressBar.setMax(10);
        }

        @Override
        protected void doneDownload() {
            progressBar.setProgress(10);
            progressBar.setBackgroundColor(Color.BLUE);
        }
    }

}
