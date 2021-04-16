package github.jomutils.android.barcode;

import android.graphics.Point;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.mlkit.vision.barcode.Barcode;

import java.util.Arrays;
import java.util.Objects;

public class BarcodeResult implements Parcelable {
    public String rawValue;
    public String displayValue;
    public int valueType;
    public int format;
    public Wifi wifi;
    public Url url;
    public Rect boundingBox;
    public Point[] cornerPoints;

    public BarcodeResult() {
    }

    public static BarcodeResult fromBarcode(Barcode barcode) {
        BarcodeResult result = new BarcodeResult();
        result.rawValue = barcode.getRawValue();
        result.valueType = barcode.getValueType();
        result.format = barcode.getFormat();
        result.displayValue = barcode.getDisplayValue();
        result.wifi = Wifi.fromBarcodeWifi(barcode.getWifi());
        result.url = Url.fromBarcodeUrl(barcode.getUrl());
        result.boundingBox = barcode.getBoundingBox();
        result.cornerPoints = barcode.getCornerPoints();

        return result;
    }

    protected BarcodeResult(Parcel in) {
        rawValue = in.readString();
        displayValue = in.readString();
        valueType = in.readInt();
        format = in.readInt();
        wifi = in.readParcelable(Wifi.class.getClassLoader());
        url = in.readParcelable(Url.class.getClassLoader());
        boundingBox = in.readParcelable(Rect.class.getClassLoader());
        cornerPoints = in.createTypedArray(Point.CREATOR);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(rawValue);
        dest.writeString(displayValue);
        dest.writeInt(valueType);
        dest.writeInt(format);
        dest.writeParcelable(wifi, flags);
        dest.writeParcelable(url, flags);
        dest.writeParcelable(boundingBox, flags);
        dest.writeTypedArray(cornerPoints, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<BarcodeResult> CREATOR = new Creator<BarcodeResult>() {
        @Override
        public BarcodeResult createFromParcel(Parcel in) {
            return new BarcodeResult(in);
        }

        @Override
        public BarcodeResult[] newArray(int size) {
            return new BarcodeResult[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BarcodeResult that = (BarcodeResult) o;
        return valueType == that.valueType &&
                format == that.format &&
                Objects.equals(rawValue, that.rawValue) &&
                Objects.equals(displayValue, that.displayValue) &&
                Objects.equals(wifi, that.wifi) &&
                Objects.equals(url, that.url) &&
                Objects.equals(boundingBox, that.boundingBox) &&
                Arrays.equals(cornerPoints, that.cornerPoints);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(rawValue, displayValue, valueType, format, wifi, url, boundingBox);
        result = 31 * result + Arrays.hashCode(cornerPoints);
        return result;
    }

    @Override
    public String toString() {
        return "BarcodeResult{" +
                "\nrawValue = '" + rawValue + '\'' +
                ", \ndisplayValue = '" + displayValue + '\'' +
                ", \nvalueType = " + valueType +
                ", \nformat = " + format +
                ", \nwifi = " + wifi +
                ", \nurl = " + url +
                ", \nboundingBox = " + boundingBox +
                ", \ncornerPoints = " + Arrays.toString(cornerPoints) +
                "\n}";
    }

    public static class Wifi implements Parcelable {
        public final String ssid;
        public final String password;
        public final int encryptionType;

        public static Wifi fromBarcodeWifi(Barcode.WiFi bcWiFi) {
            if (bcWiFi == null)
                return null;

            return new Wifi(bcWiFi.getSsid(), bcWiFi.getPassword(), bcWiFi.getEncryptionType());
        }

        public Wifi(String ssid, String password, int encryptionType) {
            this.ssid = ssid;
            this.password = password;
            this.encryptionType = encryptionType;
        }

        protected Wifi(Parcel in) {
            ssid = in.readString();
            password = in.readString();
            encryptionType = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(ssid);
            dest.writeString(password);
            dest.writeInt(encryptionType);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public static final Creator<Wifi> CREATOR = new Creator<Wifi>() {
            @Override
            public Wifi createFromParcel(Parcel in) {
                return new Wifi(in);
            }

            @Override
            public Wifi[] newArray(int size) {
                return new Wifi[size];
            }
        };

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Wifi wifi = (Wifi) o;
            return encryptionType == wifi.encryptionType &&
                    Objects.equals(ssid, wifi.ssid) &&
                    Objects.equals(password, wifi.password);
        }

        @Override
        public int hashCode() {
            return Objects.hash(ssid, password, encryptionType);
        }

        @Override
        public String toString() {
            return "Wifi{" +
                    "ssid='" + ssid + '\'' +
                    ", password='" + password + '\'' +
                    ", encryptionType=" + encryptionType +
                    '}';
        }
    }

    public static class Url implements Parcelable {
        public final String title;
        public final String url;

        public static Url fromBarcodeUrl(Barcode.UrlBookmark urlBookmark) {
            if (urlBookmark == null) {
                return null;
            }

            return new Url(urlBookmark.getTitle(), urlBookmark.getUrl());
        }

        public Url(String title, String url) {
            this.title = title;
            this.url = url;
        }

        protected Url(Parcel in) {
            title = in.readString();
            url = in.readString();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(title);
            dest.writeString(url);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public static final Creator<Url> CREATOR = new Creator<Url>() {
            @Override
            public Url createFromParcel(Parcel in) {
                return new Url(in);
            }

            @Override
            public Url[] newArray(int size) {
                return new Url[size];
            }
        };

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Url url1 = (Url) o;
            return Objects.equals(title, url1.title) &&
                    Objects.equals(url, url1.url);
        }

        @Override
        public int hashCode() {
            return Objects.hash(title, url);
        }

        @Override
        public String toString() {
            return "Url{" +
                    "title='" + title + '\'' +
                    ", url='" + url + '\'' +
                    '}';
        }
    }
}
