package com.vr.vrpano;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;




import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore.Images;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;


public class FXExplore extends Activity implements OnItemClickListener {
	private static final String TAG = "FSExplorer";
	private static final int IM_PARENT = Menu.FIRST + 1;
	private static final int IM_BACK = IM_PARENT + 1;
	ListView itemlist = null;
	String path = "/";
	List<Map<String, Object>> list;
	
	public void sendPathToActivity(String path){
		Log.i("FXExplore", "Enter sendFun");
		//Intent intent = new Intent();
		//intent.setAction("GetPath");
		//intent.putExtra("path", path);
		//sendBroadcast(intent);
		Intent intent = this.getIntent();
		intent.setClass(FXExplore.this, PlayActvity.class);
		intent.putExtra("path", path);
		startActivity(intent);
		finish();
	}
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.files);
		setTitle("文件浏览器");
		itemlist = (ListView) findViewById(R.id.itemlist);
		refreshListItems(path);
	}

	private void refreshListItems(String path) {
		setTitle("文件浏览器>"+path);
		list = buildListForSimpleAdapter(path);
		SimpleAdapter notes = new SimpleAdapter(this, list, R.layout.file_row,
				new String[] { "name", "path" ,"img", "size"}, new int[] { R.id.name,
						R.id.desc ,R.id.img, R.id.size});
		itemlist.setAdapter(notes);
		itemlist.setOnItemClickListener(this);
		itemlist.setSelection(0);
	}

	private List<Map<String, Object>> buildListForSimpleAdapter(String path) {
		File[] files = new File(path).listFiles();
		if(files == null)
		{
			List<Map<String, Object>> list = new ArrayList<Map<String, Object>>(0);
			Map<String, Object> root = new HashMap<String, Object>();
			root.put("name", "/");
			root.put("img", R.drawable.file_root);
			root.put("path", "根目录...");	//"go to root directory"
			root.put("size", "");
			list.add(root);
			Map<String, Object> pmap = new HashMap<String, Object>();
			pmap.put("name", "..");
			pmap.put("img", R.drawable.file_paranet);
			pmap.put("path", "上级目录...");	//"go to paranet Directory"
			pmap.put("size", "");
			list.add(pmap);
			return list;
		}
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>(files.length);
		Map<String, Object> root = new HashMap<String, Object>();
		root.put("name", "/");
		root.put("img", R.drawable.file_root);
		root.put("path", "根目录...");	//"go to root directory"
		root.put("size", "");
		list.add(root);
		Map<String, Object> pmap = new HashMap<String, Object>();
		pmap.put("name", "..");
		pmap.put("img", R.drawable.file_paranet);
		pmap.put("path", "上级目录...");	//"go to paranet Directory"
		pmap.put("size", "");
		list.add(pmap);
		
		for (File file : files){
			Map<String, Object> map = new HashMap<String, Object>();
			if(file.isDirectory()){
//				boolean bHasMp4 = false;
//				File[] subList = file.listFiles();
//				if (subList != null) {
//					for(File subFile : subList)
//					{
//						if (subFile.isFile()) {
//							String strFileName = subFile.getName();
//							String strExt = strFileName.substring(strFileName.lastIndexOf(".") + 1);
//							if (strExt.equals("mp4")) {
//								bHasMp4 = true;
//								break;
//							}
//						}
//					}
//
//					if (bHasMp4) {
//						map.put("img", R.drawable.video_dic);
//					}
//					else {
//						map.put("img", R.drawable.directory);
//					}
//					map.put("name", file.getName());
//					map.put("path", file.getPath());
//					//String strFileSize = FormetFileSize(file.length());
//					map.put("size", "");
//					list.add(map);
//				}

				map.put("img", R.drawable.directory);
				map.put("name", file.getName());
				map.put("path", file.getPath());
				//String strFileSize = FormetFileSize(file.length());
				map.put("size", "");
				list.add(map);
			}else{
				String strFileName = file.getName();
				String strExtName = strFileName.substring(strFileName.lastIndexOf(".") + 1);
				if (strExtName.equals("mp4")) {
					map.put("img", R.drawable.video_file_icon);
				}
				else
				{
					map.put("img", R.drawable.file_icon2);
				}
					map.put("name", file.getName());
					map.put("path", file.getPath());
					String strFileSize = FormetFileSize(file.length());
					map.put("size", strFileSize);
					list.add(map);
			}
		}
		return list;
	}

	private void goToParent() {
		File file = new File(path);
		File str_pa = file.getParentFile();
		if(str_pa == null){
			Toast.makeText(FXExplore.this,
					"已经是根目录",
					Toast.LENGTH_SHORT).show();
			refreshListItems(path);	
		}else{
			path = str_pa.getAbsolutePath();
			refreshListItems(path);	
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
		Log.i(TAG, "item clicked! [" + position + "]");
		if (position == 0) {
			path = "/";
			refreshListItems(path);
		}else if(position == 1){
			goToParent();
		} else {
			path = (String) list.get(position).get("path");
			File file = new File(path);
			if (file.isDirectory())
				refreshListItems(path);
			else
			{
				//Toast.makeText(FXExplore.this,path,Toast.LENGTH_SHORT).show();
				String strExtName = path.substring(path.lastIndexOf(".") + 1);
				if (strExtName.equals("mp4"))
				{
					sendPathToActivity(path);
					finish();
				}
				
			}
			
		}
	}
	
	public String FormetFileSize(long fileS)
	{
		DecimalFormat df = new DecimalFormat("#.00");
		String fileSizeString = "";
		if (fileS < 1024)
		{
			fileSizeString = df.format((double) fileS) + "B";
		}
		else if (fileS < 1048576)
		{
			fileSizeString = df.format((double) fileS / 1024) + "K";
		}
		else if (fileS < 1073741824)
		{
			fileSizeString = df.format((double) fileS / 1048576) + "M";
		}
		else
		{
			fileSizeString = df.format((double) fileS / 1073741824) + "G";
		}
		return fileSizeString;
	}

}