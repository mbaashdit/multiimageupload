package com.aashdit.myimageupload;

import io.realm.RealmObject;

/**
 * Created by Manabendu on 06/02/21
 */
public class Offline extends RealmObject {
    public Double latitude;
    public Double longitude;
    public String image;
    public boolean isUploaded;


}
