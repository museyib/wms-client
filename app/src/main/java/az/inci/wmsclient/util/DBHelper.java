package az.inci.wmsclient.util;

import static az.inci.wmsclient.AppConfig.DB_NAME;
import static az.inci.wmsclient.AppConfig.DB_VERSION;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import az.inci.wmsclient.model.Doc;
import az.inci.wmsclient.model.ShipDoc;
import az.inci.wmsclient.model.ShipTrx;
import az.inci.wmsclient.model.Trx;

public class DBHelper extends SQLiteOpenHelper {

    public static final String PICK_DOC = "PICK_DOC";
    public static final String PICK_TRX = "PICK_TRX";
    public static final String PACK_DOC = "PACK_DOC";
    public static final String PACK_TRX = "PACK_TRX";
    public static final String SHIP_TRX = "SHIP_TRX";
    public static final String APPROVE_DOC = "APPROVE_DOC";
    public static final String APPROVE_TRX = "APPROVE_TRX";
    public static final String INTERNAL_USE_DOC = "INTERNAL_USE_DOC";
    public static final String INTERNAL_USE_TRX = "INTERNAL_USE_TRX";

    public static final String USER_ID = "USER_ID";

    public static final String TRX_ID = "TRX_ID";
    public static final String TRX_NO = "TRX_NO";
    public static final String TRX_DATE = "TRX_DATE";
    public static final String PICK_STATUS = "PICK_STATUS";
    public static final String INV_CODE = "INV_CODE";
    public static final String INV_NAME = "INV_NAME";
    public static final String BRAND_CODE = "BRAND_CODE";
    public static final String BP_CODE = "BP_CODE";
    public static final String SBE_CODE = "SBE_CODE";
    public static final String BP_NAME = "BP_NAME";
    public static final String SBE_NAME = "SBE_NAME";
    public static final String WHS_CODE = "WHS_CODE";
    public static final String WHS_NAME = "WHS_NAME";
    public static final String EXP_CENTER_CODE = "EXP_CENTER_CODE";
    public static final String EXP_CENTER_NAME = "EXP_CENTER_NAME";
    public static final String UOM = "UOM";
    public static final String UOM_FACTOR = "UOM_FACTOR";
    public static final String QTY = "QTY";
    public static final String PICKED_QTY = "PICKED_QTY";
    public static final String PICK_AREA = "PICK_AREA";
    public static final String PICK_GROUP = "PICK_GROUP";
    public static final String PICK_USER = "PICK_USER";
    public static final String APPROVE_USER = "APPROVE_USER";
    public static final String BARCODE = "BARCODE";
    public static final String PREV_TRX_NO = "PREV_TRX_NO";
    public static final String NOTES = "NOTES";
    public static final String PRIORITY = "PRIORITY";
    public static final String STATUS = "STATUS";
    public static final String DOC_DESC = "DOC_DESC";
    public static final String ITEM_COUNT = "ITEM_COUNT";
    public static final String PICKED_ITEM_COUNT = "PICKED_ITEM_COUNT";
    public static final String REC_STATUS = "REC_STATUS";
    public static final String PACKED_QTY = "PACKED_QTY";
    public static final String REGION_CODE = "REGION_CODE";
    public static final String SRC_TRX_NO = "SRC_TRX_NO";
    public static final String VEHICLE_CODE = "VEHICLE_CODE";
    public static final String DRIVER_CODE = "DRIVER_CODE";
    public static final String DRIVER_NAME = "DRIVER_NAME";
    public static final String TRX_TYPE_ID = "TRX_TYPE_ID";
    public static final String PREV_TRX_ID = "PREV_TRX_ID";
    public static final String DISCOUNT = "DISCOUNT";
    public static final String DISCOUNT_RATIO = "DISCOUNT_RATIO";
    public static final String PRICE = "PRICE";
    public static final String AMOUNT = "AMOUNT";
    public static final String TRG_WHS_CODE = "TRG_WHS_CODE";
    public static final String TRG_WHS_NAME = "TRG_WHS_NAME";
    public static final String SRC_WHS_CODE = "SRC_WHS_CODE";
    public static final String SRC_WHS_NAME = "SRC_WHS_NAME";
    public static final String TAXED_FLAG = "TAXED_FLAG";
    public static final String ACTIVE_SECONDS = "ACTIVE_SECONDS";

    private static DBHelper instance;
    private final Logger logger;


    public DBHelper(Context context) {
        super(context, getDatabasePath(context), null, DB_VERSION);
        logger = new Logger(context);
    }

    public static synchronized DBHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DBHelper(context.getApplicationContext());
        }
        return instance;
    }

    private static String getDatabasePath(Context context) {
        File internalDb = context.getDatabasePath(DB_NAME);
        if (internalDb.exists()) {
            return internalDb.getPath();
        }

        File oldDb = new File(
                Objects.requireNonNull(context.getExternalFilesDir("/")),
                DB_NAME
        );

        if (oldDb.exists()) {
            try {
                copyFile(oldDb, internalDb);
            } catch (IOException e) {
                return oldDb.getPath();
            }
            return internalDb.getPath();
        }

        return internalDb.getPath();
    }

    private static void copyFile(File src, File dst) throws IOException {
        try (InputStream in = new FileInputStream(src);
             OutputStream out = new FileOutputStream(dst)) {

            byte[] buffer = new byte[4096];
            int len;
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createPickDocTable(db);
        createPickTrxTable(db);
        createPackDocTable(db);
        createPackTrxTable(db);
        createShipTrxTable(db);
        createApproveDocTable(db);
        createApproveTrxTable(db);
        createInternalUseDocTable(db);
        createInternalUseTrxTable(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onCreate(db);
    }

    private void createPickDocTable(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + PICK_DOC);

        StringBuilder sb = new StringBuilder();
        try {
            db.execSQL(sb.append("CREATE TABLE ")
                    .append(PICK_DOC).append("(")
                    .append(TRX_NO).append(" TEXT,")
                    .append(TRX_DATE).append(" TEXT,")
                    .append(ITEM_COUNT).append(" INTEGER,")
                    .append(PICK_GROUP).append(" TEXT,")
                    .append(PICK_AREA).append(" TEXT,")
                    .append(DOC_DESC).append(" TEXT,")
                    .append(WHS_CODE).append(" TEXT,")
                    .append(PICK_USER).append(" TEXT,")
                    .append(PICK_STATUS).append(" TEXT,")
                    .append(REC_STATUS).append(" INTEGER,")
                    .append(PREV_TRX_NO).append(" TEXT,")
                    .append(ACTIVE_SECONDS).append(" INTEGER")
                    .append(")")
                    .toString());
        } catch (Exception e) {
            logger.logError(e.toString());
        }
    }

    public List<Doc> getPickDocsByPickUser(String pickUser) {
        SQLiteDatabase db = getWritableDatabase();
        List<Doc> docList = new ArrayList<>();

        String query = "SELECT PD.TRX_NO," +
                "PD.TRX_DATE," +
                "PT_ITEM.ITEM_COUNT," +
                "PT_PICKED_ITEM.ITEM_COUNT," +
                "PD.DOC_DESC," +
                "PD.PREV_TRX_NO," +
                "PD.PICK_USER," +
                "PD.PICK_AREA," +
                "PD.PICK_GROUP, " +
                "PD.WHS_CODE, " +
                "PD.ACTIVE_SECONDS " +
                " FROM PICK_DOC PD " +
                "LEFT JOIN (SELECT TRX_NO, COUNT(DISTINCT TRX_ID) ITEM_COUNT " +
                "FROM PICK_TRX GROUP BY TRX_NO) PT_ITEM ON PD.TRX_NO=PT_ITEM.TRX_NO " +
                "LEFT JOIN (SELECT TRX_NO, COUNT(DISTINCT TRX_ID) ITEM_COUNT " +
                "FROM PICK_TRX WHERE PICKED_QTY>0 GROUP BY TRX_NO) PT_PICKED_ITEM " +
                "ON PD.TRX_NO=PT_PICKED_ITEM.TRX_NO WHERE PD.PICK_USER=?";

        try (Cursor cursor = db.rawQuery(query, new String[]{pickUser})) {
            while (cursor.moveToNext()) {
                Doc doc = new Doc();
                doc.setTrxNo(cursor.getString(0));
                doc.setTrxDate(cursor.getString(1));
                doc.setItemCount(cursor.getInt(2));
                doc.setPickedItemCount(cursor.getInt(3));
                doc.setDescription(cursor.getString(4));
                doc.setPrevTrxNo(cursor.getString(5));
                doc.setPickUser(cursor.getString(6));
                doc.setPickArea(cursor.getString(7));
                doc.setPickGroup(cursor.getString(8));
                doc.setWhsCode(cursor.getString(9));
                doc.setActiveSeconds(cursor.getInt(10));

                docList.add(doc);
            }
        }

        return docList;
    }

    public void addPickDoc(Doc doc) {
        SQLiteDatabase db = getWritableDatabase();

        String sql = "SELECT * FROM PICK_DOC WHERE TRX_NO=?";

        try (Cursor cursor = db.rawQuery(sql, new String[]{doc.getTrxNo()})) {
            if (cursor.getCount() == 0) {
                ContentValues values = new ContentValues();
                values.put(TRX_NO, doc.getTrxNo());
                values.put(TRX_DATE, doc.getTrxDate());
                values.put(ITEM_COUNT, doc.getItemCount());
                values.put(PICK_GROUP, doc.getPickGroup());
                values.put(PICK_AREA, doc.getPickArea());
                values.put(DOC_DESC, doc.getDescription());
                values.put(WHS_CODE, doc.getWhsCode());
                values.put(REC_STATUS, doc.getRecStatus());
                values.put(PICK_USER, doc.getPickUser());
                values.put(PICK_STATUS, doc.getPickStatus());
                values.put(PREV_TRX_NO, doc.getPrevTrxNo());
                values.put(ACTIVE_SECONDS, 0);

                db.insert(PICK_DOC, null, values);
            }
        }
    }

    public void updatePickActiveSeconds(String trxNo, int seconds) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(ACTIVE_SECONDS, seconds);
        db.update(PICK_DOC, values, TRX_NO + "=?", new String[]{trxNo});
    }

    public int getPickActiveSeconds(String trxNo) {
        SQLiteDatabase db = getWritableDatabase();
        try (Cursor cursor = db.rawQuery("SELECT ACTIVE_SECONDS FROM PICK_DOC WHERE TRX_NO=?",
                new String[]{trxNo})) {
            if (cursor.moveToNext()) {
                return cursor.getInt(0);
            }
        }

        return 0;
    }

    private void createPickTrxTable(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + PICK_TRX);
        StringBuilder sb = new StringBuilder();

        db.execSQL(sb.append("CREATE TABLE ")
                .append(PICK_TRX).append("(")
                .append(TRX_ID).append(" INTEGER,")
                .append(TRX_NO).append(" TEXT,")
                .append(TRX_DATE).append(" TEXT,")
                .append(PICK_STATUS).append(" TEXT,")
                .append(INV_CODE).append(" TEXT,")
                .append(INV_NAME).append(" TEXT,")
                .append(BRAND_CODE).append(" TEXT,")
                .append(BP_NAME).append(" TEXT,")
                .append(SBE_NAME).append(" TEXT,")
                .append(WHS_CODE).append(" TEXT,")
                .append(UOM).append(" TEXT,")
                .append(UOM_FACTOR).append(" REAL,")
                .append(QTY).append(" REAL,")
                .append(PICKED_QTY).append(" REAL,")
                .append(PICK_AREA).append(" TEXT,")
                .append(PICK_GROUP).append(" TEXT,")
                .append(PICK_USER).append(" TEXT,")
                .append(APPROVE_USER).append(" TEXT,")
                .append(BARCODE).append(" TEXT,")
                .append(PREV_TRX_NO).append(" TEXT,")
                .append(NOTES).append(" TEXT,")
                .append(PRIORITY).append(" INTEGER,")
                .append(STATUS).append(" INTEGER")
                .append(")")
                .toString());
    }

    public void addPickTrx(Trx trx) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(TRX_ID, trx.getTrxId());
        values.put(TRX_NO, trx.getTrxNo());
        values.put(TRX_DATE, trx.getTrxDate());
        values.put(PICK_STATUS, trx.getPickStatus());
        values.put(INV_CODE, trx.getInvCode());
        values.put(INV_NAME, trx.getInvName());
        values.put(QTY, trx.getQty());
        values.put(PICKED_QTY, trx.getPickedQty());
        values.put(WHS_CODE, trx.getWhsCode());
        values.put(PICK_AREA, trx.getPickArea());
        values.put(PICK_GROUP, trx.getPickGroup());
        values.put(PICK_USER, trx.getPickUser());
        values.put(APPROVE_USER, trx.getApproveUser());
        values.put(UOM, trx.getUom());
        values.put(UOM_FACTOR, trx.getUomFactor());
        values.put(BARCODE, trx.getBarcode());
        values.put(BP_NAME, trx.getBpName());
        values.put(SBE_NAME, trx.getSbeName());
        values.put(PREV_TRX_NO, trx.getPrevTrxNo());
        values.put(BRAND_CODE, trx.getInvBrand());
        values.put(PRIORITY, trx.getPriority());
        values.put(NOTES, trx.getNotes());
        values.put(STATUS, 0);

        db.insert(PICK_TRX, null, values);
    }

    public List<Trx> getAllPickTrx() {
        SQLiteDatabase db = getWritableDatabase();

        List<Trx> trxList = new ArrayList<>();
        String sql = "SELECT * FROM PICK_TRX";

        try (Cursor cursor = db.rawQuery(sql, new String[]{})) {
            while (cursor.moveToNext()) {
                Trx trx = new Trx();
                trx.setTrxId(cursor.getInt(0));
                trx.setTrxNo(cursor.getString(1));
                trx.setTrxDate(cursor.getString(2));
                trx.setPickStatus(cursor.getString(3));
                trx.setInvCode(cursor.getString(4));
                trx.setInvName(cursor.getString(5));
                trx.setInvBrand(cursor.getString(6));
                trx.setBpName(cursor.getString(7));
                trx.setSbeName(cursor.getString(8));
                trx.setWhsCode(cursor.getString(9));
                trx.setUom(cursor.getString(10));
                trx.setUomFactor(cursor.getDouble(11));
                trx.setQty(cursor.getDouble(12));
                trx.setPickedQty(cursor.getDouble(13));
                trx.setPickArea(cursor.getString(14));
                trx.setPickGroup(cursor.getString(15));
                trx.setPickUser(cursor.getString(16));
                trx.setApproveUser(cursor.getString(17));
                trx.setBarcode(cursor.getString(18));
                trx.setPrevTrxNo(cursor.getString(19));
                trx.setNotes(cursor.getString(20));
                trx.setPriority(cursor.getInt(21));

                if (!trxList.contains(trx)) {
                    trxList.add(trx);
                }
            }
        }
        return trxList;
    }

    public List<Trx> getPickTrx(String trxNo) {
        SQLiteDatabase db = getWritableDatabase();

        List<Trx> trxList = new ArrayList<>();

        String sql = "SELECT * FROM PICK_TRX WHERE TRX_NO=? ORDER BY INV_NAME";

        try (Cursor cursor = db.rawQuery(sql, new String[]{trxNo})) {
            int position = 0;
            while (cursor.moveToNext()) {
                Trx trx = new Trx();
                trx.setTrxId(cursor.getInt(0));
                trx.setTrxNo(cursor.getString(1));
                trx.setTrxDate(cursor.getString(2));
                trx.setPickStatus(cursor.getString(3));
                trx.setInvCode(cursor.getString(4));
                trx.setInvName(cursor.getString(5));
                trx.setInvBrand(cursor.getString(6));
                trx.setBpName(cursor.getString(7));
                trx.setSbeName(cursor.getString(8));
                trx.setWhsCode(cursor.getString(9));
                trx.setUom(cursor.getString(10));
                trx.setUomFactor(cursor.getDouble(11));
                trx.setQty(cursor.getDouble(12));
                trx.setPickedQty(cursor.getDouble(13));
                trx.setPickArea(cursor.getString(14));
                trx.setPickGroup(cursor.getString(15));
                trx.setPickUser(cursor.getString(16));
                trx.setApproveUser(cursor.getString(17));
                trx.setBarcode(cursor.getString(18));
                trx.setPrevTrxNo(cursor.getString(19));
                trx.setNotes(cursor.getString(20));
                trx.setPriority(cursor.getInt(21));
                trx.setPosition(position);

                if (!trxList.contains(trx)) {
                    trxList.add(trx);
                    position++;
                }
            }
        }
        return trxList;
    }

    public Trx getPickTrxByBarcode(String barcode, String trxNo) {
        SQLiteDatabase db = getWritableDatabase();

        String sql = "SELECT * FROM PICK_TRX WHERE BARCODE=? AND TRX_NO=?";

        Trx trx = null;
        try (Cursor cursor = db.rawQuery(sql, new String[]{barcode, trxNo})) {
            if (cursor.moveToFirst()) {
                trx = new Trx();
                trx.setTrxId(cursor.getInt(0));
                trx.setTrxNo(cursor.getString(1));
                trx.setTrxDate(cursor.getString(2));
                trx.setPickStatus(cursor.getString(3));
                trx.setInvCode(cursor.getString(4));
                trx.setInvName(cursor.getString(5));
                trx.setInvBrand(cursor.getString(6));
                trx.setBpName(cursor.getString(7));
                trx.setSbeName(cursor.getString(8));
                trx.setWhsCode(cursor.getString(9));
                trx.setUom(cursor.getString(10));
                trx.setUomFactor(cursor.getDouble(11));
                trx.setQty(cursor.getDouble(12));
                trx.setPickedQty(cursor.getDouble(13));
                trx.setPickArea(cursor.getString(14));
                trx.setPickGroup(cursor.getString(15));
                trx.setPickUser(cursor.getString(16));
                trx.setApproveUser(cursor.getString(17));
                trx.setBarcode(cursor.getString(18));
                trx.setPrevTrxNo(cursor.getString(19));
                trx.setNotes(cursor.getString(20));
                trx.setPriority(cursor.getInt(21));
            }
        }
        return trx;
    }

    public Trx getPickTrxByInvCode(String invCode, String trxNo) {
        SQLiteDatabase db = getWritableDatabase();

        String sql = "SELECT * FROM PICK_TRX WHERE INV_CODE=? AND TRX_NO=? LIMIT 1";

        Trx trx = null;
        try (Cursor cursor = db.rawQuery(sql, new String[]{invCode, trxNo})) {
            if (cursor.moveToFirst()) {
                trx = new Trx();
                trx.setTrxId(cursor.getInt(0));
                trx.setTrxNo(cursor.getString(1));
                trx.setTrxDate(cursor.getString(2));
                trx.setPickStatus(cursor.getString(3));
                trx.setInvCode(cursor.getString(4));
                trx.setInvName(cursor.getString(5));
                trx.setInvBrand(cursor.getString(6));
                trx.setBpName(cursor.getString(7));
                trx.setSbeName(cursor.getString(8));
                trx.setWhsCode(cursor.getString(9));
                trx.setUom(cursor.getString(10));
                trx.setUomFactor(cursor.getDouble(11));
                trx.setQty(cursor.getDouble(12));
                trx.setPickedQty(cursor.getDouble(13));
                trx.setPickArea(cursor.getString(14));
                trx.setPickGroup(cursor.getString(15));
                trx.setPickUser(cursor.getString(16));
                trx.setApproveUser(cursor.getString(17));
                trx.setBarcode(cursor.getString(18));
                trx.setPrevTrxNo(cursor.getString(19));
                trx.setNotes(cursor.getString(20));
                trx.setPriority(cursor.getInt(21));
            }
        }
        return trx;
    }

    public void updatePickTrx(Trx trx) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(PICKED_QTY, trx.getPickedQty());

        db.update(PICK_TRX, values, TRX_ID + "=?", new String[]{String.valueOf(trx.getTrxId())});
    }

    public void deletePickTrx(String trxNo) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(PICK_TRX, TRX_NO + "=?", new String[]{trxNo});
        db.delete(PICK_DOC, TRX_NO + "=?", new String[]{trxNo});
    }

    private void createPackDocTable(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + PACK_DOC);

        StringBuilder sb = new StringBuilder();
        try {
            db.execSQL(sb.append("CREATE TABLE ")
                    .append(PACK_DOC).append("(")
                    .append(TRX_NO).append(" TEXT,")
                    .append(TRX_DATE).append(" TEXT,")
                    .append(ITEM_COUNT).append(" INTEGER,")
                    .append(PICKED_ITEM_COUNT).append(" INTEGER,")
                    .append(PICK_GROUP).append(" TEXT,")
                    .append(PICK_AREA).append(" TEXT,")
                    .append(DOC_DESC).append(" TEXT,")
                    .append(WHS_CODE).append(" TEXT,")
                    .append(PICK_USER).append(" TEXT,")
                    .append(PICK_STATUS).append(" TEXT,")
                    .append(REC_STATUS).append(" INTEGER,")
                    .append(PREV_TRX_NO).append(" TEXT,")
                    .append(BP_CODE).append(" TEXT,")
                    .append(BP_NAME).append(" TEXT,")
                    .append(SBE_CODE).append(" TEXT,")
                    .append(SBE_NAME).append(" TEXT,")
                    .append(APPROVE_USER).append(" TEXT,")
                    .append(NOTES).append(" TEXT,")
                    .append(ACTIVE_SECONDS).append(" INTEGER")
                    .append(")")
                    .toString());
        } catch (SQLiteException e) {
            logger.logError(e.toString());
        }
    }

    public List<Doc> getPackDocsByApproveUser(String approveUser) {
        SQLiteDatabase db = getWritableDatabase();
        List<Doc> docList = new ArrayList<>();

        String query = "SELECT PD.TRX_NO," +
                "PD.TRX_DATE," +
                "PT_ITEM.ITEM_COUNT," +
                "PT_PICKED_ITEM.ITEM_COUNT," +
                "PD.DOC_DESC," +
                "PD.PREV_TRX_NO," +
                "PD.BP_CODE," +
                "PD.BP_NAME," +
                "PD.SBE_CODE," +
                "PD.SBE_NAME," +
                "PD.APPROVE_USER," +
                "PD.NOTES," +
                "PD.WHS_CODE," +
                "PD.ACTIVE_SECONDS" +
                " FROM PACK_DOC PD " +
                "LEFT JOIN (SELECT TRX_NO, COUNT(DISTINCT TRX_ID) ITEM_COUNT " +
                "FROM PACK_TRX GROUP BY TRX_NO) PT_ITEM ON PD.TRX_NO=PT_ITEM.TRX_NO " +
                "LEFT JOIN (SELECT TRX_NO, COUNT(DISTINCT TRX_ID) ITEM_COUNT " +
                "FROM PACK_TRX WHERE PICKED_QTY>0 GROUP BY TRX_NO) PT_PICKED_ITEM " +
                "ON PD.TRX_NO=PT_PICKED_ITEM.TRX_NO WHERE PD.APPROVE_USER=? ORDER BY PD.PREV_TRX_NO";

        try (Cursor cursor = db.rawQuery(query, new String[]{approveUser})) {
            while (cursor.moveToNext()) {
                Doc doc = new Doc();
                doc.setTrxNo(cursor.getString(0));
                doc.setTrxDate(cursor.getString(1));
                doc.setItemCount(cursor.getInt(2));
                doc.setPickedItemCount(cursor.getInt(3));
                doc.setDescription(cursor.getString(4));
                doc.setPrevTrxNo(cursor.getString(5));
                doc.setBpCode(cursor.getString(6));
                doc.setBpName(cursor.getString(7));
                doc.setSbeCode(cursor.getString(8));
                doc.setSbeName(cursor.getString(9));
                doc.setApproveUser(cursor.getString(10));
                doc.setNotes(cursor.getString(11));
                doc.setWhsCode(cursor.getString(12));
                doc.setActiveSeconds(cursor.getInt(13));

                docList.add(doc);
            }
        }

        return docList;
    }

    public void addPackDoc(Doc doc) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(TRX_NO, doc.getTrxNo());
        values.put(TRX_DATE, doc.getTrxDate());
        values.put(ITEM_COUNT, doc.getItemCount());
        values.put(PICKED_ITEM_COUNT, doc.getPickedItemCount());
        values.put(PICK_GROUP, doc.getPickGroup());
        values.put(PICK_AREA, doc.getPickArea());
        values.put(DOC_DESC, doc.getDescription());
        values.put(WHS_CODE, doc.getWhsCode());
        values.put(REC_STATUS, doc.getRecStatus());
        values.put(PICK_USER, doc.getPickUser());
        values.put(PICK_STATUS, doc.getPickStatus());
        values.put(PREV_TRX_NO, doc.getPrevTrxNo());
        values.put(BP_CODE, doc.getBpCode());
        values.put(BP_NAME, doc.getBpName());
        values.put(SBE_CODE, doc.getSbeCode());
        values.put(SBE_NAME, doc.getSbeName());
        values.put(APPROVE_USER, doc.getApproveUser());
        values.put(NOTES, doc.getNotes());
        values.put(ACTIVE_SECONDS, 0);

        db.delete(PACK_DOC, TRX_NO + "=?", new String[]{doc.getTrxNo()});

        db.insert(PACK_DOC, null, values);
    }

    public void updatePackActiveSeconds(String trxNo, int seconds) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(ACTIVE_SECONDS, seconds);
        db.update(PACK_DOC, values, TRX_NO + "=?", new String[]{trxNo});
    }

    public int getPackActiveSeconds(String trxNo) {
        SQLiteDatabase db = getWritableDatabase();
        try (Cursor cursor = db.rawQuery("SELECT ACTIVE_SECONDS FROM PACK_DOC WHERE TRX_NO=?",
                new String[]{trxNo})) {
            if (cursor.moveToNext()) {
                return cursor.getInt(0);
            }
        }

        return 0;
    }

    private void createPackTrxTable(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + PACK_TRX);
        StringBuilder sb = new StringBuilder();

        db.execSQL(sb.append("CREATE TABLE ")
                .append(PACK_TRX).append("(")
                .append(TRX_ID).append(" INTEGER,")
                .append(TRX_NO).append(" TEXT,")
                .append(TRX_DATE).append(" TEXT,")
                .append(PICK_STATUS).append(" TEXT,")
                .append(INV_CODE).append(" TEXT,")
                .append(INV_NAME).append(" TEXT,")
                .append(BRAND_CODE).append(" TEXT,")
                .append(BP_NAME).append(" TEXT,")
                .append(SBE_NAME).append(" TEXT,")
                .append(WHS_CODE).append(" TEXT,")
                .append(UOM).append(" TEXT,")
                .append(UOM_FACTOR).append(" REAL,")
                .append(QTY).append(" REAL,")
                .append(PICKED_QTY).append(" REAL,")
                .append(PACKED_QTY).append(" REAL,")
                .append(PICK_AREA).append(" TEXT,")
                .append(PICK_GROUP).append(" TEXT,")
                .append(PICK_USER).append(" TEXT,")
                .append(APPROVE_USER).append(" TEXT,")
                .append(BARCODE).append(" TEXT,")
                .append(PREV_TRX_NO).append(" TEXT,")
                .append(NOTES).append(" TEXT,")
                .append(PRIORITY).append(" INTEGER,")
                .append(STATUS).append(" INTEGER")
                .append(")")
                .toString());
    }

    public void addPackTrx(Trx trx) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(TRX_ID, trx.getTrxId());
        values.put(TRX_NO, trx.getTrxNo());
        values.put(TRX_DATE, trx.getTrxDate());
        values.put(PICK_STATUS, trx.getPickStatus());
        values.put(INV_CODE, trx.getInvCode());
        values.put(INV_NAME, trx.getInvName());
        values.put(QTY, trx.getQty());
        values.put(PICKED_QTY, trx.getPickedQty());
        values.put(PACKED_QTY, trx.getPackedQty());
        values.put(WHS_CODE, trx.getWhsCode());
        values.put(PICK_AREA, trx.getPickArea());
        values.put(PICK_GROUP, trx.getPickGroup());
        values.put(PICK_USER, trx.getPickUser());
        values.put(APPROVE_USER, trx.getApproveUser());
        values.put(UOM, trx.getUom());
        values.put(UOM_FACTOR, trx.getUomFactor());
        values.put(BARCODE, trx.getBarcode());
        values.put(BP_NAME, trx.getBpName());
        values.put(SBE_NAME, trx.getSbeName());
        values.put(PREV_TRX_NO, trx.getPrevTrxNo());
        values.put(BRAND_CODE, trx.getInvBrand());
        values.put(PRIORITY, trx.getPriority());
        values.put(NOTES, trx.getNotes());
        values.put(STATUS, 0);

        db.insert(PACK_TRX, null, values);
    }

    public List<Trx> getPackTrxByApproveUser(String trxNo) {
        SQLiteDatabase db = getWritableDatabase();

        List<Trx> trxList = new ArrayList<>();

        String sql = "SELECT * FROM PACK_TRX WHERE TRX_NO=? ORDER BY INV_NAME";

        try (Cursor cursor = db.rawQuery(sql, new String[]{trxNo})) {
            int position = 0;
            while (cursor.moveToNext()) {
                Trx trx = new Trx();
                trx.setTrxId(cursor.getInt(0));
                trx.setTrxNo(cursor.getString(1));
                trx.setTrxDate(cursor.getString(2));
                trx.setPickStatus(cursor.getString(3));
                trx.setInvCode(cursor.getString(4));
                trx.setInvName(cursor.getString(5));
                trx.setInvBrand(cursor.getString(6));
                trx.setBpName(cursor.getString(7));
                trx.setSbeName(cursor.getString(8));
                trx.setWhsCode(cursor.getString(9));
                trx.setUom(cursor.getString(10));
                trx.setUomFactor(cursor.getDouble(11));
                trx.setQty(cursor.getDouble(12));
                trx.setPickedQty(cursor.getDouble(13));
                trx.setPackedQty(cursor.getDouble(14));
                trx.setPickArea(cursor.getString(15));
                trx.setPickGroup(cursor.getString(16));
                trx.setPickUser(cursor.getString(17));
                trx.setApproveUser(cursor.getString(18));
                trx.setBarcode(cursor.getString(19));
                trx.setPrevTrxNo(cursor.getString(20));
                trx.setNotes(cursor.getString(21));
                trx.setPriority(cursor.getInt(22));
                trx.setPosition(position);

                if (!trxList.contains(trx)) {
                    trxList.add(trx);
                    position++;
                }
            }
        }
        return trxList;
    }

    public Trx getPackTrxByBarcode(String barcode, String trxNo) {
        SQLiteDatabase db = getWritableDatabase();

        String sql = "SELECT * FROM PACK_TRX WHERE BARCODE=? AND TRX_NO=?";

        Trx trx = null;
        try (Cursor cursor = db.rawQuery(sql, new String[]{barcode, trxNo})) {
            if (cursor.moveToFirst()) {
                trx = new Trx();
                trx.setTrxId(cursor.getInt(0));
                trx.setTrxNo(cursor.getString(1));
                trx.setTrxDate(cursor.getString(2));
                trx.setPickStatus(cursor.getString(3));
                trx.setInvCode(cursor.getString(4));
                trx.setInvName(cursor.getString(5));
                trx.setInvBrand(cursor.getString(6));
                trx.setBpName(cursor.getString(7));
                trx.setSbeName(cursor.getString(8));
                trx.setWhsCode(cursor.getString(9));
                trx.setUom(cursor.getString(10));
                trx.setUomFactor(cursor.getDouble(11));
                trx.setQty(cursor.getDouble(12));
                trx.setPickedQty(cursor.getDouble(13));
                trx.setPackedQty(cursor.getDouble(14));
                trx.setPickArea(cursor.getString(15));
                trx.setPickGroup(cursor.getString(16));
                trx.setPickUser(cursor.getString(17));
                trx.setApproveUser(cursor.getString(18));
                trx.setBarcode(cursor.getString(19));
                trx.setPrevTrxNo(cursor.getString(20));
                trx.setNotes(cursor.getString(21));
                trx.setPriority(cursor.getInt(22));
            }
        }
        return trx;
    }

    public Trx getPackTrxByInvCode(String invCode, String trxNo) {
        SQLiteDatabase db = getWritableDatabase();

        String sql = "SELECT * FROM PACK_TRX WHERE INV_CODE=? AND TRX_NO=? LIMIT 1";

        Trx trx = null;
        try (Cursor cursor = db.rawQuery(sql, new String[]{invCode, trxNo})) {
            if (cursor.moveToFirst()) {
                trx = new Trx();
                trx.setTrxId(cursor.getInt(0));
                trx.setTrxNo(cursor.getString(1));
                trx.setTrxDate(cursor.getString(2));
                trx.setPickStatus(cursor.getString(3));
                trx.setInvCode(cursor.getString(4));
                trx.setInvName(cursor.getString(5));
                trx.setInvBrand(cursor.getString(6));
                trx.setBpName(cursor.getString(7));
                trx.setSbeName(cursor.getString(8));
                trx.setWhsCode(cursor.getString(9));
                trx.setUom(cursor.getString(10));
                trx.setUomFactor(cursor.getDouble(11));
                trx.setQty(cursor.getDouble(12));
                trx.setPickedQty(cursor.getDouble(13));
                trx.setPackedQty(cursor.getDouble(14));
                trx.setPickArea(cursor.getString(15));
                trx.setPickGroup(cursor.getString(16));
                trx.setPickUser(cursor.getString(17));
                trx.setApproveUser(cursor.getString(18));
                trx.setBarcode(cursor.getString(19));
                trx.setPrevTrxNo(cursor.getString(20));
                trx.setNotes(cursor.getString(21));
                trx.setPriority(cursor.getInt(22));
            }
        }
        return trx;
    }

    public void updatePackTrx(Trx trx) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(PACKED_QTY, trx.getPackedQty());

        db.update(PACK_TRX, values, TRX_ID + "=?", new String[]{String.valueOf(trx.getTrxId())});
    }

    public void deletePackTrx(String trxNo) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(PACK_TRX, TRX_NO + "=?", new String[]{trxNo});
        db.delete(PACK_DOC, TRX_NO + "=?", new String[]{trxNo});
    }

    public void createShipTrxTable(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + SHIP_TRX);
        StringBuilder sb = new StringBuilder();

        db.execSQL(sb.append("CREATE TABLE ")
                .append(SHIP_TRX).append("(")
                .append(REGION_CODE).append(" TEXT,")
                .append(DRIVER_CODE).append(" TEXT,")
                .append(DRIVER_NAME).append(" TEXT,")
                .append(SRC_TRX_NO).append(" TEXT,")
                .append(VEHICLE_CODE).append(" TEXT,")
                .append(USER_ID).append(" TEXT,")
                .append(TAXED_FLAG).append(" TEXT")
                .append(")")
                .toString());
    }

    public void addShipTrx(ShipTrx shipTrx) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(REGION_CODE, shipTrx.getRegionCode());
        values.put(DRIVER_CODE, shipTrx.getDriverCode());
        values.put(DRIVER_NAME, shipTrx.getDriverName());
        values.put(SRC_TRX_NO, shipTrx.getSrcTrxNo());
        values.put(VEHICLE_CODE, shipTrx.getVehicleCode());
        values.put(USER_ID, shipTrx.getUserId());
        values.put(TAXED_FLAG, shipTrx.isTaxed() ? 1 : 0);

        db.insert(SHIP_TRX, null, values);
    }

    public List<ShipDoc> getShipDocs(String userId) {
        SQLiteDatabase db = getWritableDatabase();
        List<ShipDoc> shipDocList = new ArrayList<>();
        ShipDoc doc;

        try (Cursor cursor = db.rawQuery("SELECT REGION_CODE, " +
                        "DRIVER_CODE, " +
                        "DRIVER_NAME," +
                        "VEHICLE_CODE," +
                        "USER_ID," +
                        "COUNT(*) " +
                        "FROM SHIP_TRX WHERE USER_ID=? " +
                        "GROUP BY REGION_CODE, DRIVER_CODE, DRIVER_NAME, VEHICLE_CODE, USER_ID",
                new String[]{userId})) {
            while (cursor.moveToNext()) {
                doc = new ShipDoc();
                doc.setRegionCode(cursor.getString(0));
                doc.setDriverCode(cursor.getString(1));
                doc.setDriverName(cursor.getString(2));
                doc.setVehicleCode(cursor.getString(3));
                doc.setUserId(cursor.getString(4));
                doc.setCount(cursor.getInt(5));
                shipDocList.add(doc);
            }
        }

        return shipDocList;
    }

    public List<ShipTrx> getShipTrx(String driver) {
        SQLiteDatabase db = getWritableDatabase();
        List<ShipTrx> shipTrxList = new ArrayList<>();
        ShipTrx trx;

        try (Cursor cursor = db.rawQuery("SELECT * FROM SHIP_TRX WHERE DRIVER_CODE=?",
                new String[]{driver})) {
            while (cursor.moveToNext()) {
                trx = new ShipTrx();
                trx.setRegionCode(cursor.getString(0));
                trx.setDriverCode(cursor.getString(1));
                trx.setDriverName(cursor.getString(2));
                trx.setSrcTrxNo(cursor.getString(3));
                trx.setVehicleCode(cursor.getString(4));
                trx.setUserId(cursor.getString(5));
                trx.setTaxed(cursor.getInt(6) == 1);
                shipTrxList.add(trx);
            }
        }

        return shipTrxList;
    }

    public void deleteShipTrxByDriver(String driverCode) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(SHIP_TRX, DRIVER_CODE + "=?", new String[]{driverCode});
    }

    public void deleteShipTrxBySrc(String srcTrxNo) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(SHIP_TRX, SRC_TRX_NO + "=?", new String[]{srcTrxNo});
    }

    public boolean isShipped(String trxNo) {
        SQLiteDatabase db = getWritableDatabase();
        boolean shipped = false;
        try (Cursor cursor = db.rawQuery("SELECT * FROM SHIP_TRX WHERE SRC_TRX_NO=?", new String[]{trxNo})) {
            if (cursor.moveToFirst()) {
                shipped = true;
            }
        }
        return shipped;
    }

    public String barcodeList(String invCode, String tableName) {
        SQLiteDatabase db = getWritableDatabase();
        StringBuilder barcodeList = new StringBuilder();
        try (Cursor cursor = db.query(tableName,
                new String[]{BARCODE},
                INV_CODE + "=?",
                new String[]{invCode},
                BARCODE, null, null, null)) {
            while (cursor.moveToNext()) {
                barcodeList.append("\n").append(cursor.getString(0));
            }
        }
        return barcodeList.toString();
    }

    private void createApproveDocTable(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + APPROVE_DOC);
        db.execSQL(
                "CREATE TABLE APPROVE_DOC (TRX_NO TEXT PRIMARY KEY, TRX_DATE TEXT, TRX_TYPE_ID INTEGER," +
                        " AMOUNT REAL, TRG_WHS_CODE TEXT, TRG_WHS_NAME TEXT, SRC_WHS_CODE TEXT, SRC_WHS_NAME TEXT, BP_CODE TEXT, BP_NAME," +
                        " SBE_CODE TEXT, SBE_NAME TEXT, NOTES TEXT)");
    }

    public void addApproveDoc(Doc doc) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(TRX_NO, doc.getTrxNo());
        values.put(TRX_DATE, doc.getTrxDate());
        values.put(TRX_TYPE_ID, doc.getTrxTypeId());
        values.put(AMOUNT, doc.getAmount());
        values.put(TRG_WHS_CODE, doc.getWhsCode());
        values.put(TRG_WHS_NAME, doc.getWhsName());
        values.put(SRC_WHS_CODE, doc.getSrcWhsCode());
        values.put(SRC_WHS_NAME, doc.getSrcWhsName());
        values.put(BP_CODE, doc.getBpCode());
        values.put(BP_NAME, doc.getBpName());
        values.put(SBE_CODE, doc.getSbeCode());
        values.put(SBE_NAME, doc.getSbeName());
        values.put(NOTES, doc.getNotes());

        db.insert(APPROVE_DOC, null, values);
    }

    public void addProductApproveDoc(Doc doc) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(TRX_NO, doc.getTrxNo());
        values.put(TRX_DATE, doc.getTrxDate());
        values.put(NOTES, doc.getNotes());
        values.put(TRX_TYPE_ID, 4);

        db.insert(APPROVE_DOC, null, values);
    }

    public void updateApproveDoc(String trxNo, ContentValues values) {
        SQLiteDatabase db = getWritableDatabase();
        db.update(APPROVE_DOC, values, "TRX_NO=?", new String[]{trxNo});
    }

    public List<Doc> getApproveDocList() {
        SQLiteDatabase db = getWritableDatabase();
        List<Doc> docList = new ArrayList<>();

        String query = "SELECT * FROM " + APPROVE_DOC + " WHERE TRX_TYPE_ID IN (27,53)";

        try (Cursor cursor = db.rawQuery(query, null)) {
            while (cursor.moveToNext()) {
                Doc doc = new Doc();
                doc.setTrxNo(cursor.getString(0));
                doc.setTrxDate(cursor.getString(1));
                doc.setTrxTypeId(cursor.getInt(2));
                doc.setAmount(cursor.getDouble(3));
                doc.setWhsCode(cursor.getString(4));
                doc.setWhsName(cursor.getString(5));
                doc.setSrcWhsCode(cursor.getString(6));
                doc.setSrcWhsName(cursor.getString(7));
                doc.setBpCode(cursor.getString(8));
                doc.setBpName(cursor.getString(9));
                doc.setSbeCode(cursor.getString(10));
                doc.setSbeName(cursor.getString(11));
                doc.setNotes(cursor.getString(12));

                docList.add(doc);
            }
        }

        return docList;
    }

    public List<Doc> getProductApproveDocList() {
        SQLiteDatabase db = getWritableDatabase();
        List<Doc> docList = new ArrayList<>();

        String query = "SELECT * FROM " + APPROVE_DOC + " WHERE TRX_TYPE_ID = 4";

        try (Cursor cursor = db.rawQuery(query, null)) {
            while (cursor.moveToNext()) {
                Doc doc = new Doc();
                doc.setTrxNo(cursor.getString(0));
                doc.setTrxDate(cursor.getString(1));
                doc.setTrxTypeId(cursor.getInt(2));
                doc.setAmount(cursor.getDouble(3));
                doc.setWhsCode(cursor.getString(4));
                doc.setWhsName(cursor.getString(5));
                doc.setSrcWhsCode(cursor.getString(6));
                doc.setSrcWhsName(cursor.getString(7));
                doc.setBpCode(cursor.getString(8));
                doc.setBpName(cursor.getString(9));
                doc.setSbeCode(cursor.getString(10));
                doc.setSbeName(cursor.getString(11));
                doc.setNotes(cursor.getString(12));

                docList.add(doc);
            }
        }

        return docList;
    }

    public void deleteApproveDoc(String trxNo) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(APPROVE_DOC, TRX_NO + "=?", new String[]{trxNo});
        deleteApproveTrxByTrxNo(trxNo);
    }

    private void createApproveTrxTable(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + APPROVE_TRX);
        db.execSQL(
                "CREATE TABLE APPROVE_TRX (TRX_ID INTEGER PRIMARY KEY, TRX_NO TEXT, INV_CODE TEXT," +
                        " INV_NAME TEXT, BRAND_CODE TEXT, QTY REAL, PRICE REAL, AMOUNT REAL," +
                        " DISCOUNT_RATIO REAL, DISCOUNT REAL, PREV_TRX_NO TEXT, PREV_TRX_ID INTEGER," +
                        " BARCODE TEXT, NOTES TEXT)");
    }

    public void addApproveTrx(Trx trx) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(TRX_ID, getNextApproveTrxId());
        values.put(TRX_NO, trx.getTrxNo());
        values.put(INV_CODE, trx.getInvCode());
        values.put(INV_NAME, trx.getInvName());
        values.put(BRAND_CODE, trx.getInvBrand());
        values.put(QTY, trx.getQty());
        values.put(PRICE, trx.getPrice());
        values.put(AMOUNT, trx.getAmount());
        values.put(PREV_TRX_NO, trx.getPrevTrxNo());
        values.put(PREV_TRX_ID, trx.getPrevTrxId());
        values.put(DISCOUNT, trx.getDiscount());
        values.put(DISCOUNT_RATIO, trx.getDiscountRatio());
        values.put(BARCODE, trx.getBarcode());
        values.put(NOTES, trx.getNotes());

        db.insert(APPROVE_TRX, null, values);
    }

    public List<Trx> getApproveTrxList(String trxNo) {
        SQLiteDatabase db = getWritableDatabase();
        List<Trx> trxList = new ArrayList<>();

        String query = "SELECT * FROM " + APPROVE_TRX + " WHERE TRX_NO=?";

        try (Cursor cursor = db.rawQuery(query, new String[]{trxNo})) {
            while (cursor.moveToNext()) {
                Trx trx = new Trx();
                trx.setTrxId(cursor.getInt(0));
                trx.setTrxNo(cursor.getString(1));
                trx.setInvCode(cursor.getString(2));
                trx.setInvName(cursor.getString(3));
                trx.setInvBrand(cursor.getString(4));
                trx.setQty(cursor.getDouble(5));
                trx.setPrice(cursor.getDouble(6));
                trx.setAmount(cursor.getDouble(7));
                trx.setDiscountRatio(cursor.getDouble(8));
                trx.setDiscount(cursor.getDouble(9));
                trx.setPrevTrxNo(cursor.getString(10));
                trx.setPrevTrxId(cursor.getInt(11));
                trx.setBarcode(cursor.getString(12));
                trx.setNotes(cursor.getString(13));

                trxList.add(trx);
            }
        }


        return trxList;
    }

    public void deleteApproveTrxByTrxNo(String trxNo) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(APPROVE_TRX, TRX_NO + "=?", new String[]{trxNo});
    }

    public void updateApproveTrx(String invCode, ContentValues values) {
        SQLiteDatabase db = getWritableDatabase();
        db.update(APPROVE_TRX, values, TRX_ID + "=?", new String[]{invCode});
    }

    public void deleteApproveTrx(String trxId) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(APPROVE_TRX, TRX_ID + "=?", new String[]{trxId});
    }

    public int getNextApproveTrxId() {
        SQLiteDatabase db = getWritableDatabase();
        String query = "SELECT TRX_ID FROM APPROVE_TRX";
        int n;
        try (Cursor cursor = db.rawQuery(query, null, null)) {
            n = 1;
            while (cursor.moveToNext()) {
                if (n != cursor.getInt(0)) {
                    break;
                } else {
                    n++;
                }
            }
        }
        return n;
    }

    private void createInternalUseDocTable(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + INTERNAL_USE_DOC);
        db.execSQL("CREATE TABLE INTERNAL_USE_DOC (" +
                "TRX_NO TEXT PRIMARY KEY, " +
                "TRX_DATE TEXT, " +
                "TRX_TYPE_ID INTEGER," +
                "AMOUNT REAL, " +
                "WHS_CODE TEXT, " +
                "WHS_NAME TEXT, " +
                "EXP_CENTER_CODE TEXT, " +
                "EXP_CENTER_NAME TEXT, " +
                "NOTES TEXT)");
    }

    public void addInternalUseDoc(Doc doc) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(TRX_NO, doc.getTrxNo());
        values.put(TRX_DATE, doc.getTrxDate());
        values.put(TRX_TYPE_ID, doc.getTrxTypeId());
        values.put(AMOUNT, doc.getAmount());
        values.put(WHS_CODE, doc.getWhsCode());
        values.put(WHS_NAME, doc.getWhsName());
        values.put(EXP_CENTER_CODE, doc.getWhsCode());
        values.put(EXP_CENTER_NAME, doc.getWhsName());
        values.put(NOTES, doc.getNotes());

        db.insert(INTERNAL_USE_DOC, null, values);
    }

    public void updateInternalUseDoc(String trxNo, ContentValues values) {
        SQLiteDatabase db = getWritableDatabase();
        db.update(INTERNAL_USE_DOC, values, "TRX_NO=?", new String[]{trxNo});
    }

    public List<Doc> getInternalUseDocList() {
        SQLiteDatabase db = getWritableDatabase();
        List<Doc> docList = new ArrayList<>();

        String query = "SELECT * FROM " + INTERNAL_USE_DOC;

        try (Cursor cursor = db.rawQuery(query, null)) {
            while (cursor.moveToNext()) {
                Doc doc = new Doc();
                doc.setTrxNo(cursor.getString(0));
                doc.setTrxDate(cursor.getString(1));
                doc.setTrxTypeId(cursor.getInt(2));
                doc.setAmount(cursor.getDouble(3));
                doc.setWhsCode(cursor.getString(4));
                doc.setWhsName(cursor.getString(5));
                doc.setExpCenterCode(cursor.getString(6));
                doc.setExpCenterName(cursor.getString(7));
                doc.setNotes(cursor.getString(8));

                docList.add(doc);
            }
        }

        return docList;
    }

    public void deleteInternalUseDoc(String trxNo) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(INTERNAL_USE_DOC, TRX_NO + "=?", new String[]{trxNo});
        deleteInternalUseTrxByTrxNo(trxNo);
    }


    private void createInternalUseTrxTable(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + INTERNAL_USE_TRX);
        db.execSQL("CREATE TABLE INTERNAL_USE_TRX (" +
                "TRX_ID INTEGER PRIMARY KEY, " +
                "TRX_NO TEXT, " +
                "INV_CODE TEXT," +
                "INV_NAME TEXT, " +
                "BRAND_CODE TEXT, " +
                "QTY REAL, " +
                "PRICE REAL, " +
                "AMOUNT REAL," +
                "BARCODE TEXT, " +
                "NOTES TEXT)");
    }

    public void addInternalUseTrx(Trx trx) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(TRX_ID, getNextInternalUseTrxId());
        values.put(TRX_NO, trx.getTrxNo());
        values.put(INV_CODE, trx.getInvCode());
        values.put(INV_NAME, trx.getInvName());
        values.put(BRAND_CODE, trx.getInvBrand());
        values.put(QTY, trx.getQty());
        values.put(PRICE, trx.getPrice());
        values.put(AMOUNT, trx.getAmount());
        values.put(BARCODE, trx.getBarcode());
        values.put(NOTES, trx.getNotes());

        db.insert(INTERNAL_USE_TRX, null, values);
    }

    public List<Trx> getInternalUseTrxList(String trxNo) {
        SQLiteDatabase db = getWritableDatabase();
        List<Trx> trxList = new ArrayList<>();

        String query = "SELECT * FROM " + INTERNAL_USE_TRX + " WHERE TRX_NO=?";

        try (Cursor cursor = db.rawQuery(query, new String[]{trxNo})) {
            while (cursor.moveToNext()) {
                Trx trx = new Trx();
                trx.setTrxId(cursor.getInt(0));
                trx.setTrxNo(cursor.getString(1));
                trx.setInvCode(cursor.getString(2));
                trx.setInvName(cursor.getString(3));
                trx.setInvBrand(cursor.getString(4));
                trx.setQty(cursor.getDouble(5));
                trx.setPrice(cursor.getDouble(6));
                trx.setAmount(cursor.getDouble(7));
                trx.setBarcode(cursor.getString(8));
                trx.setNotes(cursor.getString(9));

                trxList.add(trx);
            }
        }


        return trxList;
    }

    public void deleteInternalUseTrx(String trxId) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(INTERNAL_USE_TRX, TRX_ID + "=?", new String[]{trxId});
    }

    public void deleteInternalUseTrxByTrxNo(String trxNo) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(INTERNAL_USE_TRX, TRX_NO + "=?", new String[]{trxNo});
    }

    public void updateInternalUseTrx(String invCode, ContentValues values) {
        SQLiteDatabase db = getWritableDatabase();
        db.update(INTERNAL_USE_TRX, values, TRX_ID + "=?", new String[]{invCode});
    }

    public int getNextInternalUseTrxId() {
        SQLiteDatabase db = getWritableDatabase();
        String query = "SELECT TRX_ID FROM INTERNAL_USE_TRX";
        int n;
        try (Cursor cursor = db.rawQuery(query, null, null)) {
            n = 1;
            while (cursor.moveToNext()) {
                if (n != cursor.getInt(0)) {
                    break;
                } else {
                    n++;
                }
            }
        }
        return n;
    }
}
