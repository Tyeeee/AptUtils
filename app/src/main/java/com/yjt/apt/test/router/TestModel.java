package com.yjt.apt.test.router;

import android.os.Parcel;
import android.os.Parcelable;

public class TestModel implements Parcelable {
    
    private String name;
    private int length;
    private int width;
    private int height;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public TestModel() {}

    @Override
    public int describeContents() { return 0; }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.name);
        dest.writeInt(this.length);
        dest.writeInt(this.width);
        dest.writeInt(this.height);
    }

    protected TestModel(Parcel in) {
        this.name = in.readString();
        this.length = in.readInt();
        this.width = in.readInt();
        this.height = in.readInt();
    }

    public static final Creator<TestModel> CREATOR = new Creator<TestModel>() {
        @Override
        public TestModel createFromParcel(Parcel source) {return new TestModel(source);}

        @Override
        public TestModel[] newArray(int size) {return new TestModel[size];}
    };
}
