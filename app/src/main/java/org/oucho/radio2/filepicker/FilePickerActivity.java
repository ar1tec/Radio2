package org.oucho.radio2.filepicker;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.oucho.radio2.R;

import java.io.File;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class FilePickerActivity extends AppCompatActivity {

	final private String[] mVideoExtensions = { "avi", "mp4", "3gp", "mov" };
	final private String[] mImagesExtensions = { "jpeg", "jpg", "png", "gif", "bmp", "wbmp" };


	private boolean mOptOnlyOneItem = false;
	private List<String> mOptFilterExclude;
	private List<String> mOptFilterListed;
	private int mOptChoiceType;
	private int mOptSortType;

	private final ImageCache mBitmapsCache = ImageCache.getInstance();

	private AbsListView mAbsListView;
	private View mEmptyView;
	private ArrayList<File> mFilesList = new ArrayList<>();
	private ArrayList<String> mSelected = new ArrayList<>();
	private final HashMap<String, Integer> mListPositioins = new HashMap<>();
	private File mCurrentDirectory;
	private boolean mIsMultiChoice = false;
	private TextView mHeaderTitle;


	private ImageButton new_folder;

    private ImageButton sort1;
    private ImageButton sort2;
    private ImageButton cancel1;
    private ImageButton cancel2;
    private ImageButton ok1;
    private ImageButton ok2;

    private View ok1_delimiter;

    private ImageButton select_all;
    private ImageButton deselect;
    private ImageButton invert;

    private Intent intent;


	@SuppressWarnings("ConstantConditions")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.fp__main_activity);


		intent = getIntent();


		mOptOnlyOneItem = intent.getBooleanExtra(FilePicker.SET_ONLY_ONE_ITEM, false);

		if (intent.hasExtra(FilePicker.SET_FILTER_EXCLUDE)) {
			mOptFilterExclude = Arrays.asList(intent.getStringArrayExtra(FilePicker.SET_FILTER_EXCLUDE));
		}

		if (intent.hasExtra(FilePicker.SET_FILTER_LISTED)) {
			mOptFilterListed = Arrays.asList(intent.getStringArrayExtra(FilePicker.SET_FILTER_LISTED));
		}

		mOptChoiceType = intent.getIntExtra(FilePicker.SET_CHOICE_TYPE, FilePicker.CHOICE_TYPE_ALL);

		mOptSortType = intent.getIntExtra(FilePicker.SET_SORT_TYPE, FilePicker.SORT_NAME_ASC);

		mEmptyView = getLayoutInflater().inflate(R.layout.fp__empty, null);
		addContentView(mEmptyView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

		setAbsListView();
		showSecondHeader(false);

		File path = null;

		if (intent.hasExtra(FilePicker.SET_START_DIRECTORY)) {
			String startPath = intent.getStringExtra(FilePicker.SET_START_DIRECTORY);
			if (startPath != null && startPath.length() > 0) {
				File tmp = new File(startPath);
				if (tmp.exists() && tmp.isDirectory()) path = tmp;
			}
		}

		if (path == null) {
			path = new File("/");
			if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) path = Environment.getExternalStorageDirectory();
		}

		readDirectory(path);

		mHeaderTitle = (TextView) findViewById(R.id.title);
		updateTitle();


		new_folder = (ImageButton) findViewById(R.id.menu_new_folder);
        newFolder();


		sort1 = (ImageButton) findViewById(R.id.menu_sort1);
		sort2 = (ImageButton) findViewById(R.id.menu_sort2);
        triRépertoire();


		cancel1 = (ImageButton) findViewById(R.id.menu_cancel1);
        setCancel1();

		cancel2 = (ImageButton) findViewById(R.id.menu_cancel2);
        setCancel2();


		ok1 = (ImageButton) findViewById(R.id.menu_ok1);
		ok1_delimiter = findViewById(R.id.ok1_delimiter);
        setOk1();

        ok2 = (ImageButton) findViewById(R.id.menu_ok2);
        setOk2();

		select_all = (ImageButton) findViewById(R.id.menu_select_all);
		deselect = (ImageButton) findViewById(R.id.menu_deselect);
		invert = (ImageButton) findViewById(R.id.menu_invert);
        setSelection();


        Log.d("Environment", String.valueOf(Environment.getExternalStorageDirectory()) );

    }



    /* ******************************************
     * Création répertoire
     * ******************************************/
    private void newFolder() {

        if (!intent.getBooleanExtra(FilePicker.DISABLE_NEW_FOLDER_BUTTON, false)) {
            new_folder.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {

                    View alertView = LayoutInflater.from(FilePickerActivity.this).inflate(R.layout.fp__new_folder, null);
                    final TextView name = (TextView) alertView.findViewById(R.id.name);

                    AlertDialog.Builder alert = new AlertDialog.Builder(FilePickerActivity.this);
                    alert.setTitle(R.string.new_folder);
                    alert.setView(alertView);
                    alert.setPositiveButton(android.R.string.ok, new Dialog.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            if (name.length() > 0) {
                                File file = new File(mCurrentDirectory, name.getText().toString());

                                if (file.exists()) {
                                    Toast.makeText(FilePickerActivity.this, R.string.folder_already_exists, Toast.LENGTH_SHORT).show();
                                } else {
                                    file.mkdir();

                                    if (file.isDirectory()) {
                                        readDirectory(mCurrentDirectory);
                                        Toast.makeText(FilePickerActivity.this, R.string.folder_created, Toast.LENGTH_SHORT).show();

                                    } else {

                                        Toast.makeText(FilePickerActivity.this, R.string.folder_not_created, Toast.LENGTH_SHORT).show();

                                    }
                                }
                            }
                        }
                    });

                    alert.setNegativeButton(android.R.string.cancel, null);
                    alert.show();

                }
            });


        } else {

                new_folder.setVisibility(ImageButton.GONE);

        }

    }


    /* ******************************************
     * Tri répertoire
     * ******************************************/

    private void triRépertoire() {

        if (!intent.getBooleanExtra(FilePicker.DISABLE_SORT_BUTTON, false)) {
            OnClickListener listener = new OnClickListener() {
                @Override
                public void onClick(View v) {
                    AlertDialog.Builder alert = new AlertDialog.Builder(FilePickerActivity.this);
                    alert.setTitle(R.string.sort);
                    alert.setItems(R.array.sorting_types, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which) {
                                case 0:
                                    mOptSortType = FilePicker.SORT_NAME_ASC;
                                    break;
                                case 1:
                                    mOptSortType = FilePicker.SORT_NAME_DESC;
                                    break;
                                case 2:
                                    mOptSortType = FilePicker.SORT_SIZE_ASC;
                                    break;
                                case 3:
                                    mOptSortType = FilePicker.SORT_SIZE_DESC;
                                    break;
                                case 4:
                                    mOptSortType = FilePicker.SORT_DATE_ASC;
                                    break;
                                case 5:
                                    mOptSortType = FilePicker.SORT_DATE_DESC;
                                    break;

                            }
                            sort();
                        }
                    });
                    alert.show();
                }
            };


            sort1.setOnClickListener(listener);
            sort2.setOnClickListener(listener);


        } else {

            sort1.setVisibility(ImageButton.GONE);
            sort2.setVisibility(ImageButton.GONE);

        }

    }

    /* ******************************************
     * Cancel1
     * ******************************************/
    private void setCancel1() {

        if (intent.getBooleanExtra(FilePicker.ENABLE_QUIT_BUTTON, false)) {
            cancel1.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    complete(null);
                }
            });

        } else {

            cancel1.setVisibility(ImageButton.GONE);
        }
    }


    /* ******************************************
     * Cancel2
     * ******************************************/
    private void setCancel2() {

        cancel2.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                disableMultiChoice();
                showSecondHeader(false);
            }

        });

    }

    /* ******************************************
     * Ok1
     * ******************************************/
    private void setOk1() {
        if (mOptOnlyOneItem && mOptChoiceType == FilePicker.CHOICE_TYPE_DIRECTORIES) {
            ok1.setVisibility(ImageButton.VISIBLE);
            ok1_delimiter.setVisibility(ImageButton.VISIBLE);
            ok1.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    ArrayList<String> list = new ArrayList<>();
                    String parent;
                    File parentFile = mCurrentDirectory.getParentFile();
                    if (parentFile == null) {
                        parent = "";
                        list.add("/");
                    } else {
                        parent = parentFile.getAbsolutePath();
                        if (!parent.endsWith("/")) parent += "/";
                        list.add(mCurrentDirectory.getName());
                    }
                    FilePickerParcelObject object = new FilePickerParcelObject(parent, list, 1);
                    complete(object);
                }
            });

        } else {
            ok1.setVisibility(ImageButton.GONE);
            ok1_delimiter.setVisibility(ImageButton.GONE);
        }
    }


    /* ******************************************
     * Ok2
     * ******************************************/
    private void setOk2() {
        ok2.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSelected.size() > 0) {
                    complete(null);
                } else {
                    disableMultiChoice();
                }
            }
        });

    }


    /* ******************************************
     * setSelection
     * ******************************************/
    private void setSelection() {
        if (mOptOnlyOneItem) {
            select_all.setVisibility(ImageButton.GONE);
            deselect.setVisibility(ImageButton.GONE);
            invert.setVisibility(ImageButton.GONE);
        } else {
            select_all.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mSelected.clear();
                    for (int i = 0; i < mFilesList.size(); i++)
                        mSelected.add(mFilesList.get(i).getName());
                    ((BaseAdapter) mAbsListView.getAdapter()).notifyDataSetChanged();
                }
            });


            deselect.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mSelected.clear();
                    ((BaseAdapter) mAbsListView.getAdapter()).notifyDataSetChanged();
                }
            });


            invert.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    ArrayList<String> tmp = new ArrayList<>();
                    for (int i = 0; i < mFilesList.size(); i++) {
                        String filename = mFilesList.get(i).getName();
                        if (!mSelected.contains(filename)) tmp.add(filename);
                    }
                    mSelected = tmp;
                    ((BaseAdapter) mAbsListView.getAdapter()).notifyDataSetChanged();
                }
            });

        }
    }



	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
			if (event.getAction() == KeyEvent.ACTION_UP) {
				if (mIsMultiChoice) {
					disableMultiChoice();
				} else {
					File parentFile = mCurrentDirectory.getParentFile();
					if (parentFile == null) {
						complete(null);
					} else {
						readDirectory(parentFile);

						String path = mCurrentDirectory.getAbsolutePath();
						if (mListPositioins.containsKey(path)) {
							mAbsListView.setSelection(mListPositioins.get(path));
							mListPositioins.remove(path);
						}

						updateTitle();
					}
				}
			} else if (event.getAction() == KeyEvent.ACTION_DOWN && (event.getFlags() & KeyEvent.FLAG_LONG_PRESS) == KeyEvent.FLAG_LONG_PRESS) {
				mSelected.clear();
				complete(null);
			}
			return true;
		}
		return super.dispatchKeyEvent(event);
	}

	private void disableMultiChoice() {
		showSecondHeader(false);
		mIsMultiChoice = false;
		mSelected.clear();
		if (mOptChoiceType == FilePicker.CHOICE_TYPE_FILES && !mOptOnlyOneItem) {
			readDirectory(mCurrentDirectory);
		}
		((BaseAdapter) mAbsListView.getAdapter()).notifyDataSetChanged();
	}

	private void showSecondHeader(boolean show) {
		if (show) {
			findViewById(R.id.header1).setVisibility(View.GONE);
			findViewById(R.id.header2).setVisibility(View.VISIBLE);
		} else {
			findViewById(R.id.header1).setVisibility(View.VISIBLE);
			findViewById(R.id.header2).setVisibility(View.GONE);
		}
	}

	private void updateTitle() {

		mHeaderTitle.setText(mCurrentDirectory.getName());
        //mHeaderTitle.setText(mCurrentDirectory.getPath());

/*        ImageView updir = (ImageView) findViewById(R.id.menu_updir);


        if (mCurrentDirectory.getName().equals("")){
            updir.setVisibility(View.GONE);
        } else {
            updir.setVisibility(View.VISIBLE);
        }*/
	}

	private void complete(FilePickerParcelObject object) {
		if (object == null) {
			String path = mCurrentDirectory.getAbsolutePath();
			if (!path.endsWith("/")) path += "/";
			object = new FilePickerParcelObject(path, mSelected, mSelected.size());
		}
		Intent intent = new Intent();
		intent.putExtra(FilePickerParcelObject.class.getCanonicalName(), object);
		setResult(RESULT_OK, intent);
		finish();
	}

	private void readDirectory(File path) {
		mCurrentDirectory = path;
		mFilesList.clear();
		File[] files = path.listFiles();
		if (files != null) {
			for (int i = 0; i < files.length; i++) {
				if (mOptChoiceType == FilePicker.CHOICE_TYPE_DIRECTORIES && !files[i].isDirectory()) continue;
				if (files[i].isFile()) {
					String extension = getFileExtension(files[i].getName());
					if (mOptFilterListed != null && !mOptFilterListed.contains(extension)) continue;
					if (mOptFilterExclude != null && mOptFilterExclude.contains(extension)) continue;
				}
				mFilesList.add(files[i]);
			}
		}

		sort();
	}

	private void sort() {
		Collections.sort(mFilesList, new Comparator<File>() {

			@Override
			public int compare(File file1, File file2) {

				boolean isDirectory1 = file1.isDirectory();
				boolean isDirectory2 = file2.isDirectory();

				if (isDirectory1 && !isDirectory2)
					return -1;

				if (!isDirectory1 && isDirectory2)
					return 1;

				switch (mOptSortType) {
				case FilePicker.SORT_NAME_DESC:
					return file2.getName().toLowerCase(Locale.getDefault()).compareTo(file1.getName().toLowerCase(Locale.getDefault()));
				case FilePicker.SORT_SIZE_ASC:
					return Long.valueOf(file1.length()).compareTo(file2.length());
				case FilePicker.SORT_SIZE_DESC:
					return Long.valueOf(file2.length()).compareTo(file1.length());
				case FilePicker.SORT_DATE_ASC:
					return Long.valueOf(file1.lastModified()).compareTo(file2.lastModified());
				case FilePicker.SORT_DATE_DESC:
					return Long.valueOf(file2.lastModified()).compareTo(file1.lastModified());
				}
				// Default, FilePicker.SORT_NAME_ASC
				return file1.getName().toLowerCase(Locale.getDefault()).compareTo(file2.getName().toLowerCase(Locale.getDefault()));
			}
		});
		((BaseAdapter) mAbsListView.getAdapter()).notifyDataSetChanged();
	}


	private void setAbsListView() {

		mAbsListView = (AbsListView) findViewById(R.id.listview);
		mAbsListView.setEmptyView(mEmptyView);
		FilesListAdapter adapter = new FilesListAdapter(this, R.layout.fp__list_item);

        mAbsListView.setAdapter(adapter);

		mAbsListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if (position < mFilesList.size()) {
					File file = mFilesList.get(position);
					if (mIsMultiChoice) {
						CheckBox checkBox = (CheckBox) view.findViewById(R.id.checkbox);
						if (checkBox.isChecked()) {
							checkBox.setChecked(false);
							mSelected.remove(file.getName());
						} else {
							if (mOptOnlyOneItem) {
								mSelected.clear();
								((BaseAdapter) mAbsListView.getAdapter()).notifyDataSetChanged();
							}
							checkBox.setChecked(true);
							mSelected.add(file.getName());
						}
					} else {
						if (file.isDirectory()) {
							int currentPosition = mAbsListView.getFirstVisiblePosition();
							mListPositioins.put(mCurrentDirectory.getAbsolutePath(), currentPosition);
							readDirectory(file);
							updateTitle();
							mAbsListView.setSelection(0);
						} else {
							mSelected.add(file.getName());
							complete(null);
						}
					}
				}
			}
		});

		if (mOptChoiceType != FilePicker.CHOICE_TYPE_FILES || !mOptOnlyOneItem) {

            if (!mOptOnlyOneItem) {

                mAbsListView.setOnItemLongClickListener(new OnItemLongClickListener() {

                    @Override
                    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                        if (!mIsMultiChoice) {
                            mIsMultiChoice = true;
                            if (position < mFilesList.size()) {
                                File file = mFilesList.get(position);
                                if (mOptChoiceType != FilePicker.CHOICE_TYPE_FILES || file.isFile())
                                    mSelected.add(file.getName());
                            }

                            if (mOptChoiceType == FilePicker.CHOICE_TYPE_FILES && !mOptOnlyOneItem) {
                                ArrayList<File> tmpList = new ArrayList<>();
                                for (int i = 0; i < mFilesList.size(); i++) {
                                    File file = mFilesList.get(i);
                                    if (file.isFile()) tmpList.add(file);
                                }
                                mFilesList = tmpList;
                            }

                            ((BaseAdapter) mAbsListView.getAdapter()).notifyDataSetChanged();

                            showSecondHeader(true);
                            return true;
                        }
                        return false;
                    }
                });

            }

        }

		mAbsListView.setVisibility(View.VISIBLE);
	}


	@SuppressLint("DefaultLocale")
	private String getFileExtension(String fileName) {
		int index = fileName.lastIndexOf(".");
		if (index == -1) return "";
		return fileName.substring(index + 1, fileName.length()).toLowerCase(Locale.getDefault());
	}


	private void addBitmapToCache(String key, Bitmap bitmap) {
		if (getBitmapFromCache(key) == null) {
			mBitmapsCache.put(key, bitmap);
		}
	}

	private Bitmap getBitmapFromCache(String key) {
		return mBitmapsCache.get(key);
	}

	class FilesListAdapter extends BaseAdapter {
		private final Context mContext;
		private final int mResource;

		public FilesListAdapter(Context context, int resource) {
			mContext = context;
			mResource = resource;
		}

		@Override
		public int getCount() {
			return mFilesList.size();
		}

		@Override
		public Object getItem(int position) {
			return mFilesList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

        @Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			File file = mFilesList.get(position);

			FileTypeUtils.FileType fileType = FileTypeUtils.getFileType(file);

			convertView = LayoutInflater.from(mContext).inflate(mResource, parent, false);

			ImageView thumbnail = (ImageView) convertView.findViewById(R.id.thumbnail);
            TextView filesize = (TextView) convertView.findViewById(R.id.filesize);
            TextView filesize2 = (TextView) convertView.findViewById(R.id.filesize2);

            CheckBox checkbox = (CheckBox) convertView.findViewById(R.id.checkbox);

			if (mSelected.contains(file.getName())) {
				checkbox.setChecked(true);
			} else {
				checkbox.setChecked(false);
			}

			if (mIsMultiChoice) {
                checkbox.setVisibility(View.VISIBLE);
                filesize.setVisibility(View.INVISIBLE);
                filesize2.setVisibility(View.VISIBLE);

            }

			if (file.isDirectory()) {
				thumbnail.setImageResource(R.drawable.ic_folder);

			} else {

				if ((Arrays.asList(mVideoExtensions).contains(getFileExtension(file.getName()))
                        || Arrays.asList(mImagesExtensions).contains(getFileExtension(file.getName())))) {

					Bitmap bitmap = getBitmapFromCache(file.getAbsolutePath());
					if (bitmap == null) {
                        new ThumbnailLoader(thumbnail).execute(file);
                    } else {
                        thumbnail.setImageBitmap(bitmap);
                    }

				} else {

                    thumbnail.setImageResource(fileType.getIcon());
                }
			}

			TextView filename = (TextView) convertView.findViewById(R.id.filename);
			filename.setText(file.getName());



			if (filesize != null) {

                String date = getDateModification(file);
                String taille = getHumanFileSize(file.length());


				if (file.isFile()) {
					filesize.setText(taille + "  " + date);
                    filesize2.setText(taille + "  " + date);

                } else {
					filesize.setText("<dir> " + date);
                    filesize2.setText("<dir> " + date);
				}
			}

			return convertView;
		}

		String getHumanFileSize(long size) {
			String[] units = getResources().getStringArray(R.array.size_units);
			for (int i = units.length - 1; i >= 0; i--) {
				if (size >= Math.pow(1024, i)) {
					return Math.round((size / Math.pow(1024, i))) + " " + units[i];
				}
			}
			return size + " " + units[0];
		}

        String getDateModification(File file) {
            Date lastModified = new Date(file.lastModified());
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy   HH:mm");

            return formatter.format(lastModified);
        }

		class ThumbnailLoader extends AsyncTask<File, Void, Bitmap> {
			private final WeakReference<ImageView> imageViewReference;

			public ThumbnailLoader(ImageView imageView) {
				imageViewReference = new WeakReference<>(imageView);
			}

			@Override
			protected Bitmap doInBackground(File... arg0) {
				Bitmap thumbnailBitmap = null;
				File file = arg0[0];
                if (file != null) {
                    try {
						ContentResolver crThumb = getContentResolver();
						if (Arrays.asList(mVideoExtensions).contains(getFileExtension(file.getName()))) {
                            Cursor cursor = crThumb.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, new String[] { MediaStore.Video.Media._ID }, MediaStore.Video.Media.DATA + "='" + file.getAbsolutePath() + "'", null, null);
							if (cursor != null) {
                                if (cursor.getCount() > 0) {
                                    cursor.moveToFirst();
									thumbnailBitmap = MediaStore.Video.Thumbnails.getThumbnail(crThumb, cursor.getInt(0), MediaStore.Video.Thumbnails.MICRO_KIND, null);
								}
								cursor.close();
							}
						} else if (Arrays.asList(mImagesExtensions).contains(getFileExtension(file.getName()))) {
                            Cursor cursor = crThumb.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, new String[] { MediaStore.Images.Media._ID }, MediaStore.Images.Media.DATA + "='" + file.getAbsolutePath() + "'", null, null);
							if (cursor != null) {
                                if (cursor.getCount() > 0) {
                                    cursor.moveToFirst();
									thumbnailBitmap = MediaStore.Images.Thumbnails.getThumbnail(crThumb, cursor.getInt(0), MediaStore.Images.Thumbnails.MINI_KIND, null);
								}
								cursor.close();
							}
						}
                    } catch (Exception | Error e) {
						e.printStackTrace();
					}
                }
                if (thumbnailBitmap != null) addBitmapToCache(file.getAbsolutePath(), thumbnailBitmap);
				return thumbnailBitmap;
			}

			@Override
			protected void onPostExecute(Bitmap bitmap) {
				if (imageViewReference != null) {
					final ImageView imageView = imageViewReference.get();
					if (imageView != null) {
						if (bitmap == null) imageView.setImageResource(R.drawable.ic_file_gray_116dp);
						else imageView.setImageBitmap(bitmap);
					}
				}
			}

		}

	}


}
