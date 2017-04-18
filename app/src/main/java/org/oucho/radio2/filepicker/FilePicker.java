package org.oucho.radio2.filepicker;

public class FilePicker {
	public final static String SET_ONLY_ONE_ITEM = "OnlyOneItem";
	final static String SET_FILTER_EXCLUDE = "FilterExclude";
	public final static String SET_FILTER_LISTED = "FilterListed";
	public final static String SET_CHOICE_TYPE = "ViewType";
	public final static String SET_START_DIRECTORY = "StartDirectory";
	final static String SET_SORT_TYPE = "SortType";
	public final static String DISABLE_NEW_FOLDER_BUTTON = "DisableNewFolderButton";
	public final static String DISABLE_SORT_BUTTON = "DisableSortButton";
	public final static String ENABLE_QUIT_BUTTON = "EnableCancelButton";

	final static int CHOICE_TYPE_ALL = 0;
	public final static int CHOICE_TYPE_FILES = 1;
	final static int CHOICE_TYPE_DIRECTORIES = 2;

	final static int SORT_NAME_ASC = 0;
	final static int SORT_NAME_DESC = 1;
	final static int SORT_SIZE_ASC = 2;
	final static int SORT_SIZE_DESC = 3;
	final static int SORT_DATE_ASC = 4;
	final static int SORT_DATE_DESC = 5;
}