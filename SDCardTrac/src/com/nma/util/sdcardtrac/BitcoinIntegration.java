package com.nma.util.sdcardtrac;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.widget.Toast;

/**
 * This class has been extracted from 'Bitcoin Wallet's implementation
 * All credits to http://wallet.schildbach.de
 * Original source at https://github.com/schildbach/bitcoin-wallet
 */

public class BitcoinIntegration {
    /**
     * Request any amount of Bitcoins (probably a donation) from user, without feedback from the app.
     *
     * @param context
     *            Android context
     * @param address
     *            Bitcoin address
     */
    public static void request(final Context context, final String address)
    {
        final Intent intent = makeBitcoinUriIntent(address, null);

        start(context, intent);
    }


    /**
     * Request any amount of Bitcoins (probably a donation) from user, with feedback from the app. Result intent can be
     * received by overriding {@link android.app.Activity#onActivityResult()}. Result indicates either
     * {@link android.app.Activity#RESULT_OK} or {@link android.app.Activity#RESULT_CANCELED}. In the success case, use
     * {@link #transactionHashFromResult(Intent)} to read the transaction hash from the intent.
     *
     * Warning: A success indication is no guarantee! To be on the safe side, you must drive your own Bitcoin
     * infrastructure and validate the transaction.
     *
     * @param activity
     *            Calling Android activity
     * @param requestCode
     *            Code identifying the call when {@link android.app.Activity#onActivityResult()} is called back
     * @param address
     *            Bitcoin address
     */
    public static void requestForResult(final Activity activity, final int requestCode, final String address)
    {
        final Intent intent = makeBitcoinUriIntent(address, null);

        startForResult(activity, requestCode, intent);
    }


    private static final int SATOSHIS_PER_COIN = 100000000;

    private static Intent makeBitcoinUriIntent(final String address, final Long amount)
    {
        final StringBuilder uri = new StringBuilder("bitcoin:");
        if (address != null)
            uri.append(address);
        if (amount != null)
            uri.append("?amount=").append(String.format("%d.%08d", amount / SATOSHIS_PER_COIN, amount % SATOSHIS_PER_COIN));

        final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri.toString()));

        return intent;
    }

    private static void start(final Context context, final Intent intent)
    {
        final PackageManager pm = context.getPackageManager();
        if (pm.resolveActivity(intent, 0) != null)
            context.startActivity(intent);
        else
            redirectToDownload(context);
    }

    private static void startForResult(final Activity activity, final int requestCode, final Intent intent)
    {
        final PackageManager pm = activity.getPackageManager();
        if (pm.resolveActivity(intent, 0) != null)
            activity.startActivityForResult(intent, requestCode);
        else
            redirectToDownload(activity);
    }

    private static void redirectToDownload(final Context context)
    {
        Toast.makeText(context, "No Bitcoin application found.\nPlease install Bitcoin Wallet.", Toast.LENGTH_LONG).show();

        final Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=de.schildbach.wallet"));
        final Intent binaryIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/schildbach/bitcoin-wallet/releases"));

        final PackageManager pm = context.getPackageManager();
        if (pm.resolveActivity(marketIntent, 0) != null)
            context.startActivity(marketIntent);
        else if (pm.resolveActivity(binaryIntent, 0) != null)
            context.startActivity(binaryIntent);
        // else out of luck
    }
}
