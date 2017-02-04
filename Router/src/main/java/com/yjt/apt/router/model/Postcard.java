package com.yjt.apt.router.model;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.util.SparseArray;

import com.yjt.apt.router.Router;
import com.yjt.apt.router.annotation.model.RouteMetadata;
import com.yjt.apt.router.listener.callback.NavigationCallback;
import com.yjt.apt.router.listener.template.IProvider;

import java.io.Serializable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;

public final class Postcard extends RouteMetadata {

    private Uri uri;
    private Object tag;             // A tag prepare for some thing wrong.
    private Bundle bundle;         // Data to tranform
    private int flag = -1;         // Flags of route
    private int timeout = 300;      // Navigation timeout, TimeUnit.Second !
    private IProvider provider;     // It will be set value, if this postcard was provider.
    private boolean isGreenChannal;

    public Postcard() {
        this(null, null);
    }

    public Postcard(String path, String group) {
        this(path, group, null, null);
    }

    public Postcard(String path, String group, Uri uri, Bundle bundle) {
        setPath(path);
        setGroup(group);
        setUri(uri);
        this.bundle = (null == bundle ? new Bundle() : bundle);
    }


    public IProvider getProvider() {
        return provider;
    }

    public Postcard setProvider(IProvider provider) {
        this.provider = provider;
        return this;
    }

    public boolean isGreenChannal() {
        return isGreenChannal;
    }

    public Object getTag() {
        return tag;
    }

    public Postcard setTag(Object tag) {
        this.tag = tag;
        return this;
    }

    public Bundle getExtras() {
        return bundle;
    }

    public int getTimeout() {
        return timeout;
    }

    public Postcard setTimeout(int timeout) {
        this.timeout = timeout;
        return this;
    }

    public Uri getUri() {
        return uri;
    }

    public Postcard setUri(Uri uri) {
        this.uri = uri;
        return this;
    }

//    public Object navigation() {
//        return navigation(null);
//    }

    public Object navigation(Context context) {
        return navigation(context, null);
    }

    public Object navigation(Context context, NavigationCallback callback) {
        return Router.getInstance().navigation(context, this, -1, callback);
    }

    public void navigation(Activity mContext, int requestCode) {
        navigation(mContext, requestCode, null);
    }

    public void navigation(Activity mContext, int requestCode, NavigationCallback callback) {
        Router.getInstance().navigation(mContext, this, requestCode, callback);
    }

    public Postcard setGreenChannel() {
        this.isGreenChannal = true;
        return this;
    }

    public Postcard putBundle(Bundle bundle) {
        if (null != bundle) {
            this.bundle = bundle;
        }
        return this;
    }

    @IntDef({
            Intent.FLAG_ACTIVITY_SINGLE_TOP,
            Intent.FLAG_ACTIVITY_NEW_TASK,
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            Intent.FLAG_DEBUG_LOG_RESOLUTION,
            Intent.FLAG_FROM_BACKGROUND,
            Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT,
            Intent.FLAG_ACTIVITY_CLEAR_TASK,
            Intent.FLAG_ACTIVITY_CLEAR_TOP,
            Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS,
            Intent.FLAG_ACTIVITY_FORWARD_RESULT,
            Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY,
            Intent.FLAG_ACTIVITY_MULTIPLE_TASK,
            Intent.FLAG_ACTIVITY_NO_ANIMATION,
            Intent.FLAG_ACTIVITY_NO_USER_ACTION,
            Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP,
            Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED,
            Intent.FLAG_ACTIVITY_REORDER_TO_FRONT,
            Intent.FLAG_ACTIVITY_TASK_ON_HOME,
            Intent.FLAG_RECEIVER_REGISTERED_ONLY
    })
    @Retention(RetentionPolicy.SOURCE)
    @interface FlagInt {}

    public Postcard setFlag(@FlagInt int flag) {
        this.flag = flag;
        return this;
    }

    public int getFlag() {
        return flag;
    }

    public Postcard putString(@Nullable String key, @Nullable String value) {
        bundle.putString(key, value);
        return this;
    }

    public Postcard putBoolean(@Nullable String key, boolean value) {
        bundle.putBoolean(key, value);
        return this;
    }

    public Postcard putShort(@Nullable String key, short value) {
        bundle.putShort(key, value);
        return this;
    }

    public Postcard putInt(@Nullable String key, int value) {
        bundle.putInt(key, value);
        return this;
    }

    public Postcard putLong(@Nullable String key, long value) {
        bundle.putLong(key, value);
        return this;
    }

    public Postcard putDouble(@Nullable String key, double value) {
        bundle.putDouble(key, value);
        return this;
    }

    public Postcard putByte(@Nullable String key, byte value) {
        bundle.putByte(key, value);
        return this;
    }

    public Postcard putChar(@Nullable String key, char value) {
        bundle.putChar(key, value);
        return this;
    }

    public Postcard putFloat(@Nullable String key, float value) {
        bundle.putFloat(key, value);
        return this;
    }

    public Postcard putCharSequence(@Nullable String key, @Nullable CharSequence value) {
        bundle.putCharSequence(key, value);
        return this;
    }

    public Postcard putParcelable(@Nullable String key, @Nullable Parcelable value) {
        bundle.putParcelable(key, value);
        return this;
    }

    public Postcard putParcelableArray(@Nullable String key, @Nullable Parcelable[] value) {
        bundle.putParcelableArray(key, value);
        return this;
    }

    public Postcard putParcelableArrayList(@Nullable String key, @Nullable ArrayList<? extends Parcelable> value) {
        bundle.putParcelableArrayList(key, value);
        return this;
    }

    public Postcard putSparseParcelableArray(@Nullable String key, @Nullable SparseArray<? extends Parcelable> value) {
        bundle.putSparseParcelableArray(key, value);
        return this;
    }

    public Postcard putIntegerArrayList(@Nullable String key, @Nullable ArrayList<Integer> value) {
        bundle.putIntegerArrayList(key, value);
        return this;
    }

    public Postcard putStringArrayList(@Nullable String key, @Nullable ArrayList<String> value) {
        bundle.putStringArrayList(key, value);
        return this;
    }

    public Postcard putCharSequenceArrayList(@Nullable String key, @Nullable ArrayList<CharSequence> value) {
        bundle.putCharSequenceArrayList(key, value);
        return this;
    }

    public Postcard putSerializable(@Nullable String key, @Nullable Serializable value) {
        bundle.putSerializable(key, value);
        return this;
    }

    public Postcard putByteArray(@Nullable String key, @Nullable byte[] value) {
        bundle.putByteArray(key, value);
        return this;
    }

    public Postcard putShortArray(@Nullable String key, @Nullable short[] value) {
        bundle.putShortArray(key, value);
        return this;
    }

    public Postcard putCharArray(@Nullable String key, @Nullable char[] value) {
        bundle.putCharArray(key, value);
        return this;
    }

    public Postcard putFloatArray(@Nullable String key, @Nullable float[] value) {
        bundle.putFloatArray(key, value);
        return this;
    }

    public Postcard putCharSequenceArray(@Nullable String key, @Nullable CharSequence[] value) {
        bundle.putCharSequenceArray(key, value);
        return this;
    }

    public Postcard putBundle(@Nullable String key, @Nullable Bundle value) {
        bundle.putBundle(key, value);
        return this;
    }

    @Override
    public String toString() {
        return "Postcard:" + super.toString();
    }
}
