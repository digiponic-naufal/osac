package osac.digiponic.com.osac.Model;

public class DataItemMenu {

    private String _itemID;
    private String _itemName;
    private String _itemPrice;
    private String _itemVehicleType;
    private String _itemType;
    private boolean selected = false;

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public DataItemMenu() {
    }

    public DataItemMenu(String _itemName, String _itemPrice) {
        this._itemName = _itemName;
        this._itemPrice = _itemPrice;
    }

    public DataItemMenu(String _itemID, String _itemName, String _itemPrice, String _itemVehicleType, String _itemType) {
        this._itemID = _itemID;
        this._itemName = _itemName;
        this._itemPrice = _itemPrice;
        this._itemVehicleType = _itemVehicleType;
        this._itemType = _itemType;
    }

    public String get_itemName() {
        return _itemName;
    }

    public void set_itemName(String _itemName) {
        this._itemName = _itemName;
    }

    public int get_itemPrice() {
        return Integer.parseInt(_itemPrice);
    }

    public void set_itemPrice(String _itemPrice) {
        this._itemPrice = _itemPrice;
    }

    public String get_itemID() {
        return _itemID;
    }

    public void set_itemID(String _itemID) {
        this._itemID = _itemID;
    }

    public String get_itemVehicleType() {
        return _itemVehicleType;
    }

    public void set_itemVehicleType(String _itemVehicleType) {
        this._itemVehicleType = _itemVehicleType;
    }

    public String get_itemType() {
        return _itemType;
    }

    public void set_itemType(String _itemType) {
        this._itemType = _itemType;
    }
}
