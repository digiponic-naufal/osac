package osac.digiponic.com.osac.view.ui;

import android.Manifest;
import android.app.Dialog;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.hardware.camera2.TotalCaptureResult;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.cooltechworks.views.shimmer.ShimmerRecyclerView;
import com.google.gson.JsonIOException;
import com.zj.btsdk.BluetoothService;
import com.zj.btsdk.PrintPic;

import net.glxn.qrgen.android.QRCode;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import butterknife.OnClick;
import fr.castorflex.android.smoothprogressbar.SmoothProgressBar;
import osac.digiponic.com.osac.R;
import osac.digiponic.com.osac.print.BluetoothHandler;
import osac.digiponic.com.osac.print.DeviceActivity;
import osac.digiponic.com.osac.view.adapter.InvoiceRVAdapter;
import osac.digiponic.com.osac.view.adapter.MenuRVAdapter;
import osac.digiponic.com.osac.model.DataItemMenu;
import osac.digiponic.com.osac.model.DataServiceType;
import osac.digiponic.com.osac.model.DataVehicleType;
import osac.digiponic.com.osac.print.DeviceList;
import osac.digiponic.com.osac.print.PrinterCommands;
import osac.digiponic.com.osac.print.Utils;
import osac.digiponic.com.osac.viewmodel.MainActivityViewModel;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements MenuRVAdapter.ItemClickListener, EasyPermissions.PermissionCallbacks, BluetoothHandler.HandlerInterface {

    // Dataset
    private List<DataItemMenu> mDataItem = new ArrayList<>();
    private List<DataItemMenu> mDataCart = new ArrayList<>();
    private List<DataItemMenu> dataCarWash = new ArrayList<>();
    private List<DataItemMenu> dataCarCare = new ArrayList<>();
    private List<DataVehicleType> mDataVehicleType = new ArrayList<>();
    private List<DataServiceType> mDataServiceType = new ArrayList<>();

    // View Model
    private MainActivityViewModel mMainActivityViewModel;

    // Content
    private RecyclerView recyclerView_Menu, recyclerView_carWash, recyclerView_carCare, recyclerView_Invoice;
    private ImageView emptyCart;
    private Spinner typeFilter;
    private TextView total_tv, date_tv, hidden_tv;
    private Dialog completeDialog, incompleteDialog, changeTypeDialog;
    private Button smallCar, mediumCar, bigCar, checkOutBtn;
    private SmoothProgressBar progressBar;
    private LinearLayout blackLayout;

    // Adapter
    private MenuRVAdapter menuRVAdapter;
    private MenuRVAdapter carWashRVAdapter;
    private MenuRVAdapter carCareRVAdapter;
    private InvoiceRVAdapter invoiceRVAdapter;

    // Variable
    byte FONT_TYPE;
    private static BluetoothSocket btsocket;
    private static OutputStream outputStream;
    int total = 0;

    // Variable Global
    private String POLICE_NUMBER = "N1440TRW";
    private String VEHICLE_TYPE;
    private String BRAND;
    private String VEHICLE_NAME;
    private String DISCOUNT_TYPE = "Nominal";
    private int DISCOUNT = 0;
    private int TOTALPRICE = total;

    // Constraint
    public static final int CONNECTION_TIMEOUT = 10000;
    public static final int READ_TIMEOUT = 15000;

    private ShimmerRecyclerView recommended_shimmer;
    private ShimmerRecyclerView carWash_shimmer;
    private ShimmerRecyclerView carCare_shimmer;

    // Bluetooth Printer
    public static final int RC_BLUETOOTH = 0;
    public static final int RC_CONNECT_DEVICE = 1;
    public static final int RC_ENABLE_BLUETOOTH = 2;
    private BluetoothService mService = null;
    private boolean isPrinterReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Lock Screen to Horizontal
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_main);

        recommended_shimmer = findViewById(R.id.recommended_shimmer_recyclerView);
        carWash_shimmer = findViewById(R.id.car_wash_shimmer_recyclerView);
        carCare_shimmer = findViewById(R.id.car_care_shimmer_recyclerView);

        recommended_shimmer.showShimmerAdapter();
        carWash_shimmer.showShimmerAdapter();
        carCare_shimmer.showShimmerAdapter();

        Bundle extras = getIntent().getExtras();
        Log.d("setelahdiangirim", String.valueOf(extras.get("VEHICLE_TYPE")));
        if (extras == null) {
            VEHICLE_TYPE = null;
            BRAND = null;
            VEHICLE_NAME = null;
        } else {
            VEHICLE_TYPE = extras.getString("VEHICLE_TYPE");
            BRAND = extras.getString("BRAND");
            VEHICLE_NAME = extras.getString("VEHICLE_NAME");
        }
        Toast.makeText(this, VEHICLE_TYPE, Toast.LENGTH_SHORT).show();

        // Initialize View Model
        mMainActivityViewModel = ViewModelProviders.of(this).get(MainActivityViewModel.class);
        mMainActivityViewModel.init(VEHICLE_TYPE);

        // Get Data From View Model
//        mMainActivityViewModel.getmMenuData().observe(this, dataItemMenus -> {
//            // Notify Adapter
//            menuRVAdapter.notifyDataSetChanged();
//        });

//        mDataVehicleType = mMainActivityViewModel.getmVehicleData().getValue();
//        assert mDataVehicleType != null;
//        for (DataVehicleType item : mDataVehicleType) {
//            Log.d("itemtipe", item.getId());
//            Log.d("itemtipe", item.getName());
//            Log.d("itemtipe", item.getTypes());
//        }

//        mMainActivityViewModel.getmServiceData().observe(this, dataServiceTypes -> {
//        });
//
//        mMainActivityViewModel.getmVehicleData().observe(this, dataVehicleTypes -> {
//        });

        // set Adapter
        setAdapterRV();

        setupBluetooth();


        // Initialize TextView Total
        total_tv = findViewById(R.id.total_textview_main);
        total_tv.setText("Rp. 0");

        // Initialize TextView Date
        date_tv = findViewById(R.id.date);
        Date date = Calendar.getInstance().getTime();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
        String formattedDate = dateFormat.format(date);
        date_tv.setText(formattedDate);

        // Setup Dialog
        completeDialog = new Dialog(this);
        setupCompleteDialog();
        incompleteDialog = new Dialog(this);
        setupUnCompleteDialog();
        changeTypeDialog = new Dialog(this);
        setupChangeTypeDialog();

        // Initialize Invoice Recyclerview Placeholder
        emptyCart = findViewById(R.id.img_emptyCart);

        // Setup Progress Bar
        progressBar = findViewById(R.id.progress_bar);
        blackLayout = findViewById(R.id.loading_layout);

        // Setup Hidden Function
        hidden_tv = findViewById(R.id.textView_invoice);
        hidden_tv.setOnLongClickListener(v -> {
            Toast.makeText(MainActivity.this, "Setup Page", Toast.LENGTH_SHORT).show();
            toSetting();
            return false;
        });

        // Set Checkout Button
        checkOutBtn = findViewById(R.id.btn_checkout);
        checkOutBtn.setOnClickListener(v -> {
//            printImage(R.drawable.downloadwhite);
//            printTextNew();
            printInvoice();
//            blackLayout.setVisibility(View.VISIBLE);
//            new Handler().postDelayed(() -> {
//                blackLayout.setVisibility(View.GONE);
//                if (mDataCart.size() == 0) {
//                    incompleteDialog.show();
//                } else {
//                    new HTTPAsyncTaskPOSTData().execute("http://app.digiponic.co.id/osac/apiosac/api/transaksi");
//                    total_tv.setText("Rp. 0");
//                }
//
//                Log.d("datacartsize", String.valueOf(mDataCart.size()));
//            }, 3000);
        });
    }

    // Method bellow are used to support printing for thermal printer

    @AfterPermissionGranted(RC_BLUETOOTH)
    private void setupBluetooth() {
        String[] params = {Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN};
        if (!EasyPermissions.hasPermissions(this, params)) {
            EasyPermissions.requestPermissions(this, "You need bluetooth permission",
                    RC_BLUETOOTH, params);
            return;
        }
        mService = new BluetoothService(this, new BluetoothHandler(this));
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
    }

    @Override
    public void onDeviceConnected() {
        isPrinterReady = true;
//        tvStatus.setText("Terhubung dengan perangkat");
    }

    @Override
    public void onDeviceConnecting() {
//        tvStatus.setText("Sedang menghubungkan...");
    }

    @Override
    public void onDeviceConnectionLost() {
        isPrinterReady = false;
//        tvStatus.setText("Koneksi perangkat terputus");
    }

    @Override
    public void onDeviceUnableToConnect() {
//        tvStatus.setText("Tidak dapat terhubung ke perangkat");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case RC_ENABLE_BLUETOOTH:
                if (resultCode == RESULT_OK) {
//                    Log.i(TAG, "onActivityResult: bluetooth aktif");
                } else
//                    Log.i(TAG, "onActivityResult: bluetooth harus aktif untuk menggunakan fitur ini");
                    break;
            case RC_CONNECT_DEVICE:
                if (resultCode == RESULT_OK) {
                    String address = data.getExtras().getString(DeviceActivity.EXTRA_DEVICE_ADDRESS);
                    BluetoothDevice mDevice = mService.getDevByMac(address);
                    mService.connect(mDevice);
                }
                break;
        }
    }

    public void printInvoice() {
        if (!mService.isAvailable()) {
            return;
        }
        if (isPrinterReady) {
            Date date = Calendar.getInstance().getTime();
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
            SimpleDateFormat monthName = new SimpleDateFormat("dd MMMM yyyy");
            String formattedDate = monthName.format(date);

            // Print
            printUnicode();
            printImage(R.drawable.downloadwhite);
            printTextNew(formattedDate);
            printQRCode("test");
            printUnicode();
            printNewLine();
            printNewLine();

        } else {
            if (mService.isBTopen())
                startActivityForResult(new Intent(this, DeviceActivity.class), RC_CONNECT_DEVICE);
            else
                requestBluetooth();
        }
    }

    public void printNewLine() {
        mService.write(PrinterCommands.FEED_LINE);
    }

    public void printTextNew(String text) {
        if (!mService.isAvailable()) {
//            Log.i(TAG, "printText: perangkat tidak support bluetooth");
            return;
        }
        if (isPrinterReady) {
            if (text == null) {
                Toast.makeText(this, "Cant print null text", Toast.LENGTH_SHORT).show();
                return;
            }
            mService.write(PrinterCommands.ESC_ALIGN_CENTER);
            mService.sendMessage(text, "");
            mService.write(PrinterCommands.ESC_ENTER);
        } else {
            if (mService.isBTopen())
                startActivityForResult(new Intent(this, DeviceActivity.class), RC_CONNECT_DEVICE);
            else
                requestBluetooth();
        }
    }

    public void printImage(int img) {
        if (isPrinterReady) {
            Bitmap bmp = BitmapFactory.decodeResource(getResources(),
                    img);
            Bitmap resizedImage = Bitmap.createScaledBitmap(bmp, (int) (bmp.getWidth() * 0.8), (int) (bmp.getHeight() * 0.8), true);
            if (resizedImage != null) {
                byte[] command = Utils.decodeBitmap(resizedImage);
                mService.write(PrinterCommands.ESC_ALIGN_CENTER);
                mService.write(command);
            }

        } else {
            if (mService.isBTopen())
                startActivityForResult(new Intent(this, DeviceActivity.class), RC_CONNECT_DEVICE);
            else
                requestBluetooth();
        }
    }

    private void requestBluetooth() {
        if (mService != null) {
            if (!mService.isBTopen()) {
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intent, RC_ENABLE_BLUETOOTH);
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();


    }

    private void toSetting() {
//        Intent BTIntent = new Intent(getApplicationContext(), DeviceList.class);
//        this.startActivityForResult(BTIntent, DeviceList.REQUEST_CONNECT_BT);
        startActivityForResult(new Intent(this, DeviceActivity.class), RC_CONNECT_DEVICE);
    }


//    private void printInvoice() {
//        if (btsocket == null) {
//            Toast.makeText(this, "No Bluetooth Setup Found.", Toast.LENGTH_SHORT).show();
////            Intent BTIntent = new Intent(getApplicationContext(), DeviceList.class);
////            this.startActivityForResult(BTIntent, DeviceList.REQUEST_CONNECT_BT);
//            startActivityForResult(new Intent(this, DeviceActivity.class), RC_CONNECT_DEVICE);
//        } else {
//            OutputStream opstream = null;
//            try {
//                opstream = btsocket.getOutputStream();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            outputStream = opstream;
//
//            //print command
//            try {
//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                outputStream = btsocket.getOutputStream();
//
//                byte[] printformat = {0x1B, 0 * 21, FONT_TYPE};
//                //outputStream.write(printformat);
//
//                Date date = Calendar.getInstance().getTime();
//                SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
//                SimpleDateFormat monthName = new SimpleDateFormat("dd MMMM yyyy");
//                String formattedDate = monthName.format(date);
//
//                // Print
//                printUnicode();
//                printPhoto(R.drawable.downloadwhite);
//                printCustom(formattedDate, 0, 1);
////                printQRCode(message.getText().toString());
//                printUnicode();
//                printNewLine();
//                printNewLine();
//
//
//                outputStream.flush();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//    }

    //print custom
    private void printCustom(String msg, int size, int align) {
        //Print config "mode"
        byte[] cc = new byte[]{0x1B, 0x21, 0x03};  // 0- normal size text
        //byte[] cc1 = new byte[]{0x1B,0x21,0x00};  // 0- normal size text
        byte[] bb = new byte[]{0x1B, 0x21, 0x08};  // 1- only bold text
        byte[] bb2 = new byte[]{0x1B, 0x21, 0x20}; // 2- bold with medium text
        byte[] bb3 = new byte[]{0x1B, 0x21, 0x10}; // 3- bold with large text
        try {
            switch (size) {
                case 0:
                    outputStream.write(cc);
                    break;
                case 1:
                    outputStream.write(bb);
                    break;
                case 2:
                    outputStream.write(bb2);
                    break;
                case 3:
                    outputStream.write(bb3);
                    break;
            }

            switch (align) {
                case 0:
                    //left align
                    outputStream.write(PrinterCommands.ESC_ALIGN_LEFT);
                    break;
                case 1:
                    //center align
                    outputStream.write(PrinterCommands.ESC_ALIGN_CENTER);
                    break;
                case 2:
                    //right align
                    outputStream.write(PrinterCommands.ESC_ALIGN_RIGHT);
                    break;
            }
            outputStream.write(msg.getBytes());
            outputStream.write(PrinterCommands.LF);
            //outputStream.write(cc);
            //printNewLine();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    //print photo
    public void printPhoto(int img) {
        try {
            Bitmap bmp = BitmapFactory.decodeResource(getResources(),
                    img);
//            Bitmap resized = Bitmap.createBitmap(bmp, 0, 0, 200, 200);
            Bitmap resizeImage = Bitmap.createScaledBitmap(bmp, (int) (bmp.getWidth() * 0.8), (int) (bmp.getHeight() * 0.8), true);
            if (bmp != null) {
                byte[] command = Utils.decodeBitmap(resizeImage);
                outputStream.write(PrinterCommands.ESC_ALIGN_CENTER);
                printText(command);
            } else {
                Log.e("Print Photo error", "the file isn't exists");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("PrintTools", "the file isn't exists");
        }
    }

//    public void printQRCode(String text) {
//        try {
//            Bitmap bmp = QRCode.from(text).bitmap();
//            Bitmap resizeImage = Bitmap.createScaledBitmap(bmp, (int) (bmp.getWidth() * 2), (int) (bmp.getHeight() * 2), true);
//            if (bmp != null) {
//                byte[] command = Utils.decodeBitmap(resizeImage);
//                outputStream.write(PrinterCommands.ESC_ALIGN_CENTER);
//                printText(command);
//            } else {
//                Log.e("Print Photo error", "the file isn't exists");
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            Log.e("PrintTools", "the file isn't exists");
//        }
//    }

    public void printQRCode(String text) {
        try {
            Bitmap bmp = QRCode.from(text).bitmap();
            Bitmap resizedImage = Bitmap.createScaledBitmap(bmp, (int) (bmp.getWidth() * 2), (int) (bmp.getHeight() * 2), true);
            if (resizedImage != null) {
                byte[] command = Utils.decodeBitmap(resizedImage);
                mService.write(PrinterCommands.ESC_ALIGN_CENTER);
                mService.write(command);
            } else {
                Log.e("Print Photo error", "the file isn't exists");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("PrintTools", "the file isn't exists");
        }
    }

//    //print unicode
//    public void printUnicode() {
//        try {
//            outputStream.write(PrinterCommands.ESC_ALIGN_CENTER);
//            printText(Utils.UNICODE_TEXT);
//        } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    //print unicode
    public void printUnicode() {
        mService.write(PrinterCommands.ESC_ALIGN_CENTER);
        printTextNew("#####################");
    }


    public static void resetPrint() {
        try {
            outputStream.write(PrinterCommands.ESC_FONT_COLOR_DEFAULT);
            outputStream.write(PrinterCommands.FS_FONT_ALIGN);
            outputStream.write(PrinterCommands.ESC_ALIGN_LEFT);
            outputStream.write(PrinterCommands.ESC_CANCEL_BOLD);
            outputStream.write(PrinterCommands.LF);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //print text
    private void printText(String msg) {
        try {
            // Print normal text
            outputStream.write(msg.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    //print byte[]
    private void printText(byte[] msg) {
        try {
            // Print normal text
            outputStream.write(msg);
            printNewLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private String leftRightAlign(String str1, String str2) {
        String ans = str1 + str2;
        if (ans.length() < 31) {
            int n = (31 - str1.length() + str2.length());
            ans = str1 + new String(new char[n]).replace("\0", " ") + str2;
        }
        return ans;
    }


    private String[] getDateTime() {
        final Calendar c = Calendar.getInstance();
        String dateTime[] = new String[2];
        dateTime[0] = c.get(Calendar.DAY_OF_MONTH) + "/" + c.get(Calendar.MONTH) + "/" + c.get(Calendar.YEAR);
        dateTime[1] = c.get(Calendar.HOUR_OF_DAY) + ":" + c.get(Calendar.MINUTE);
        return dateTime;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (btsocket != null) {
                outputStream.close();
                btsocket.close();
                btsocket = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        try {
//            btsocket = DeviceList.getSocket();
//            if (btsocket != null) {
////                printText(message.getText().toString());
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    //==========================================================================================================================


    @Override
    public void onItemClick(View view, int position) {
        Log.d("itempositiondebug", String.valueOf(position));
        Log.d("itempositionname", String.valueOf(menuRVAdapter.getItemName(position) + menuRVAdapter.getItemPrice(position)));
        Locale localeID = new Locale("in", "ID");
        NumberFormat formatRupiah = NumberFormat.getCurrencyInstance(localeID);
        total = 0;
        if (!menuRVAdapter.isSelected(position)) {
            mDataCart.add(new DataItemMenu(menuRVAdapter.getItemID(position), menuRVAdapter.getItemName(position),
                    menuRVAdapter.getItemPrice(position), menuRVAdapter.getItemVehicleType(position),
                    menuRVAdapter.getItemType(position), menuRVAdapter.getItemImage(position)));
            invoiceRVAdapter.notifyDataSetChanged();
            menuRVAdapter.setSelected(position, true);
            menuRVAdapter.notifyDataSetChanged();
        } else {
            for (int i = 0; i < mDataCart.size(); i++) {
                if (mDataCart.get(i).get_itemName().equalsIgnoreCase(menuRVAdapter.getItemName(position))) {
                    invoiceRVAdapter.removeAt(i);
                    invoiceRVAdapter.notifyItemRemoved(i);
                    menuRVAdapter.setSelected(position, false);
                    menuRVAdapter.notifyDataSetChanged();
                    break;
                }
            }
        }
        Log.d("datacartsizeadd", String.valueOf(mDataCart.size()));

        for (DataItemMenu item : mDataCart) {
            total += item.get_itemPrice();
        }
        if (total > 0) {
            total_tv.setText(formatRupiah.format((double) total));
        }
    }

    @Override
    public void onCarWashItemClick(View view, int position) {
        Log.d("itempositiondebug", String.valueOf(position));
        Log.d("itempositionname", String.valueOf(carWashRVAdapter.getItemName(position) + carWashRVAdapter.getItemPrice(position)));
        Locale localeID = new Locale("in", "ID");
        NumberFormat formatRupiah = NumberFormat.getCurrencyInstance(localeID);
        total = 0;
        if (!carWashRVAdapter.isSelected(position)) {
            mDataCart.add(new DataItemMenu(carWashRVAdapter.getItemID(position), carWashRVAdapter.getItemName(position),
                    carWashRVAdapter.getItemPrice(position), carWashRVAdapter.getItemVehicleType(position),
                    carWashRVAdapter.getItemType(position), carWashRVAdapter.getItemImage(position)));
            invoiceRVAdapter.notifyDataSetChanged();
            carWashRVAdapter.setSelected(position, true);
            carWashRVAdapter.notifyDataSetChanged();
        } else {
            for (int i = 0; i < mDataCart.size(); i++) {
                if (mDataCart.get(i).get_itemName().equalsIgnoreCase(carWashRVAdapter.getItemName(position))) {
                    invoiceRVAdapter.removeAt(i);
                    invoiceRVAdapter.notifyItemRemoved(i);
                    carWashRVAdapter.setSelected(position, false);
                    carWashRVAdapter.notifyDataSetChanged();
                    break;
                }
            }
        }
        Log.d("datacartsizeadd", String.valueOf(mDataCart.size()));

        for (DataItemMenu item : mDataCart) {
            total += item.get_itemPrice();
        }
        if (total > 0) {
            total_tv.setText(formatRupiah.format((double) total));
        }
    }

    @Override
    public void onCarCareItemClick(View view, int position) {
        Log.d("itempositiondebug", String.valueOf(position));
        Log.d("itempositionname", String.valueOf(carCareRVAdapter.getItemName(position) + carCareRVAdapter.getItemPrice(position)));
        Locale localeID = new Locale("in", "ID");
        NumberFormat formatRupiah = NumberFormat.getCurrencyInstance(localeID);
        total = 0;
        if (!carCareRVAdapter.isSelected(position)) {
            mDataCart.add(new DataItemMenu(carCareRVAdapter.getItemID(position), carCareRVAdapter.getItemName(position),
                    carCareRVAdapter.getItemPrice(position), carCareRVAdapter.getItemVehicleType(position),
                    carCareRVAdapter.getItemType(position), carCareRVAdapter.getItemImage(position)));
            invoiceRVAdapter.notifyDataSetChanged();
            carCareRVAdapter.setSelected(position, true);
            carCareRVAdapter.notifyDataSetChanged();
        } else {
            for (int i = 0; i < mDataCart.size(); i++) {
                if (mDataCart.get(i).get_itemName().equalsIgnoreCase(carCareRVAdapter.getItemName(position))) {
                    invoiceRVAdapter.removeAt(i);
                    invoiceRVAdapter.notifyItemRemoved(i);
                    carCareRVAdapter.setSelected(position, false);
                    carCareRVAdapter.notifyDataSetChanged();
                    break;
                }
            }
        }
        Log.d("datacartsizeadd", String.valueOf(mDataCart.size()));

        for (DataItemMenu item : mDataCart) {
            total += item.get_itemPrice();
        }
        if (total > 0) {
            total_tv.setText(formatRupiah.format((double) total));
        }
    }

    private void setAdapterRV() {
        // Setup Menu Recyclerview
        recyclerView_Menu = findViewById(R.id.rv_recommended);
        recyclerView_Menu.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        menuRVAdapter = new MenuRVAdapter(this, new ArrayList<>());
        menuRVAdapter.setClickListener(this);

        recyclerView_carWash = findViewById(R.id.rv_car_wash);
        recyclerView_carWash.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        carWashRVAdapter = new MenuRVAdapter(this, mMainActivityViewModel.getmMenuDataWash().getValue());
        carWashRVAdapter.setCarWashIitemClickListener(this);

        recyclerView_carCare = findViewById(R.id.rv_car_care);
        recyclerView_carCare.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        carCareRVAdapter = new MenuRVAdapter(this, mMainActivityViewModel.getmMenuDataCare().getValue());
        carCareRVAdapter.setCarCareItemClickListener(this);

        // Load Data to UI
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Remove Shimmer
                recommended_shimmer.hideShimmerAdapter();
                carWash_shimmer.hideShimmerAdapter();
                carCare_shimmer.hideShimmerAdapter();

                recyclerView_Menu.setAdapter(menuRVAdapter);
                menuRVAdapter.notifyDataSetChanged();
                recyclerView_carWash.setAdapter(carWashRVAdapter);
                carWashRVAdapter.notifyDataSetChanged();
                recyclerView_carCare.setAdapter(carCareRVAdapter);
                carCareRVAdapter.notifyDataSetChanged();
            }
        }, 5000);

        // Setup Invoice Recyclerview
        recyclerView_Invoice = findViewById(R.id.rv_invoiceItem);
        recyclerView_Invoice.setLayoutManager(new LinearLayoutManager(this));
        invoiceRVAdapter = new InvoiceRVAdapter(this, mDataCart);
        invoiceRVAdapter.setClickListener(this);
        invoiceRVAdapter.notifyDataSetChanged();
        recyclerView_Invoice.setAdapter(invoiceRVAdapter);
        invoiceRVAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                checkEmpty();
            }

            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                checkEmpty();
            }

            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                super.onItemRangeRemoved(positionStart, itemCount);
                checkEmpty();
            }
        });
    }

    // Post Data
    public class HTTPAsyncTaskPOSTData extends AsyncTask<String, Void, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }

        @Override
        protected String doInBackground(String... urls) {
            // params comes from the execute() call: params[0] is the url.
            try {
                try {
                    return HttpPost(urls[0]);
                } catch (JSONException e) {
                    e.printStackTrace();
                    return "Error!";
                }
            } catch (IOException e) {
                return "Unable to retrieve web page. URL may be invalid.";
            }
        }

        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            printInvoice();
            dataCartClear();
            completeDialog.show();

        }

        private String HttpPost(String myUrl) throws IOException, JSONException {
            String result = "";
            URL url = new URL(myUrl);

            // 1. create HttpURLConnection
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            Log.d("responseCodePost", String.valueOf(conn.getResponseCode()));

            // 2. build JSON object
            JSONObject jsonObject = createJSON();

            // 3. add JSON content to POST request body
            setPostRequestContent(conn, jsonObject);

            // 4. make POST request to the given URL
            conn.connect();

            // 5. return response message
            return conn.getResponseMessage() + "";
        }

        private void setPostRequestContent(HttpURLConnection conn,
                                           JSONObject jsonObject) throws IOException {
            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            writer.write(jsonObject.toString());
            Log.i(MainActivity.class.toString(), jsonObject.toString());
            writer.flush();
            writer.close();
            os.close();
        }
    }

    private JSONObject createJSON() throws JSONException {
        JSONObject jsonObject = new JSONObject();

        // Get IDs
        int typesID = 0, serviceID = 0;
        String nama;


        int total_price = total;
        int discount = 0;
        if (mDataCart.size() > 0) {
            for (DataItemMenu item : mDataCart) {
                total_price += item.get_itemPrice();
            }
        }

        try {

            // Get ID's
//            for (DataVehicleType item : mDataVehicleType) {
//                Log.d("itemtipe", item.getId());
//                Log.d("itemtipe", item.getName());
//                Log.d("itemtipe", item.getTypes());
//            }

            // Data Masih Hardcoded
            String jenisKendaraan = null;
            switch (VEHICLE_TYPE) {
                case "25":
                    jenisKendaraan = "Besar";
                    break;
                case "26":
                    jenisKendaraan = "Kecil";
                    break;
                case "27":
                    jenisKendaraan = "Sedang";
                    break;
            }

            // Add Property
            jsonObject.accumulate("no_polisi", POLICE_NUMBER);
            jsonObject.accumulate("jenis_kendaraan", jenisKendaraan);
            jsonObject.accumulate("merek_kendaraan", BRAND);
            jsonObject.accumulate("nama_kendaraan", VEHICLE_NAME);
            jsonObject.accumulate("subtotal", total);
            jsonObject.accumulate("diskon_tipe", DISCOUNT_TYPE);
            jsonObject.accumulate("diskon", 0);
            jsonObject.accumulate("total", total);

            //JsonArr
            JSONArray jsonArray = new JSONArray();
            for (DataItemMenu item : mDataCart) {
                JSONObject pnObj = new JSONObject();
                pnObj.accumulate("nama", item.get_itemName());
                pnObj.accumulate("harga", item.get_itemPrice());
                pnObj.accumulate("kategori", item.get_itemType());
                jsonArray.put(pnObj);
            }
            jsonObject.accumulate("penjualan_detail", jsonArray);
            Log.d("EXPORTJSON", jsonObject.toString());
        } catch (JsonIOException e) {
            Log.d("JSONERROREX", e.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    /*
        Method bellow for supporting purpose
     */

    private void checkEmpty() {
        emptyCart.setVisibility(invoiceRVAdapter.getItemCount() > 0 ? View.GONE : View.VISIBLE);
    }

    private void dataCartClear() {
        mDataCart.clear();
        for (DataItemMenu item : mDataItem) {
            item.setSelected(false);
        }
        invoiceRVAdapter.notifyDataSetChanged();
        menuRVAdapter.notifyDataSetChanged();
        total_tv.setText("");
    }

    private void setupCompleteDialog() {
        completeDialog.setContentView(R.layout.dialog_done);
        completeDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        completeDialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        Button okBtn = completeDialog.findViewById(R.id.dialog_ok_btn);
        okBtn.setOnClickListener(v -> completeDialog.dismiss());
    }

    private void setupUnCompleteDialog() {
        incompleteDialog.setContentView(R.layout.dialog_undone);
        incompleteDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        incompleteDialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        Button okBtn = incompleteDialog.findViewById(R.id.dialog_ok_btn_undone);
        okBtn.setOnClickListener(v -> incompleteDialog.dismiss());
    }

    private void setupChangeTypeDialog() {
        changeTypeDialog.setContentView(R.layout.change_type_dialog);
        changeTypeDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        changeTypeDialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        Button cancelBtn = changeTypeDialog.findViewById(R.id.dialog_cancel_btn_change);
        Button okBtn = changeTypeDialog.findViewById(R.id.dialog_ok_btn_change);
        cancelBtn.setOnClickListener(v -> {
            changeTypeDialog.dismiss();
        });
        okBtn.setOnClickListener(v -> {
            changeTypeDialog.dismiss();
        });
    }
}
