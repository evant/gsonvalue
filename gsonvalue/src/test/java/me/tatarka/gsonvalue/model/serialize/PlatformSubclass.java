package me.tatarka.gsonvalue.model.serialize;

import android.os.Parcelable;
import me.tatarka.gsonvalue.annotations.GsonValue;

@GsonValue
public class PlatformSubclass implements Parcelable {

    private final int arg;

    public PlatformSubclass(int arg) {
        this.arg = arg;
    }

    public int getArg() {
        return arg;
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
