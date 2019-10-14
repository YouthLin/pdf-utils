package com.youthlin.pdf.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.itextpdf.text.exceptions.BadPasswordException;
import com.itextpdf.text.pdf.PdfReader;
import com.youthlin.pdf.App;
import com.youthlin.pdf.model.Bookmark;
import com.youthlin.pdf.model.FileListItem;
import com.youthlin.pdf.model.TreeTableBookmarkItem;
import com.youthlin.pdf.util.FxUtil;
import com.youthlin.pdf.util.Jsons;
import com.youthlin.pdf.util.PdfUtil;
import com.youthlin.pdf.util.Strings;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.PasswordField;
import javafx.scene.control.SelectionModel;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static com.youthlin.utils.i18n.Translation.__;
import static com.youthlin.utils.i18n.Translation._f;

/**
 * @author youthlin.chen
 * @date 2019-10-12 20:11
 */
@Slf4j
public class MainController implements Initializable {
    public TabPane tabPane;

    public Tab bookmarkTab;
    public Button open;
    public Label fileName;
    public TreeTableView<TreeTableBookmarkItem> treeTableView;
    public GridPane grid;
    public Label titleLabel;
    public TextField titleInput;
    public Label pageLabel;
    public TextField pageInput;
    public Button addButton;
    public Button addChildButton;
    public Button editButton;
    public Button deleteButton;
    public TextArea jsonTextArea;
    public Button save;
    public Button resetAll;
    public Button transFromJson;

    public Tab mergeTab;
    public ListView<FileListItem> listView;
    public Button addFile;
    public Button removeItem;
    public Button moveTop;
    public Button moveUp;
    public Button moveDown;
    public Button moveBottom;
    public Button merge;

    public Tab passTab;
    public Button selectSrc;
    public Label srcFile;
    public Label passwordLabel;
    public PasswordField passwordField;
    public Button updatePassword;

    public Tab aboutTab;

    public Label statusLabel;

    private static final String FILENAME_TIP = __("Click left button to open a PDF file");
    private File bookmarkSrcFile;
    private byte[] bookmarkFilePass;
    private File passwordSrcFile;
    private byte[] passwordFileOldPass;
    private Bookmark bookmark;
    public Stage stage;
    public App app;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        tabPane.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            statusLabel.setText(newValue.getText());
        });

        bookmarkTab.setText(__("Pdf Bookmark Editor"));
        open.setText(__("Open File"));
        fileName.setText(FILENAME_TIP);
        titleLabel.setText(__("Title"));
        pageLabel.setText(__("Page"));
        addButton.setText(__("Add Item"));
        addChildButton.setText(__("Add Child Item"));
        editButton.setText(__("Update"));
        deleteButton.setText(__("Delete"));
        save.setText(__("Write Bookmarks"));
        resetAll.setText(__("Reset"));
        transFromJson.setText(__("Trans From Input Json"));

        mergeTab.setText(__("Pdf Merger"));
        addFile.setText(__("Add File"));
        removeItem.setText(__("Remove"));
        moveTop.setText(__("Move Top"));
        moveUp.setText(__("Move Up"));
        moveDown.setText(__("Move Down"));
        moveBottom.setText(__("Move Bottom"));
        merge.setText(__("Merge"));

        passTab.setText(__("Pdf Password"));
        selectSrc.setText(__("Open File"));
        srcFile.setText(FILENAME_TIP);
        passwordLabel.setText(__("Password"));
        updatePassword.setText(__("Save as"));

        aboutTab.setText(__("About"));

        statusLabel.setText(__("Ready"));

        initTree();
        initList();
    }

    private void initTree() {
        treeTableView.setRoot(new TreeItem<>());
        treeTableView.setShowRoot(false);
        TreeTableColumn<TreeTableBookmarkItem, String> titleColumn = new TreeTableColumn<>(__("Title"));
        TreeTableColumn<TreeTableBookmarkItem, Integer> pageColumn = new TreeTableColumn<>(__("Page"));
        titleColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("title"));
        titleColumn.setSortable(false);
        pageColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("page"));
        pageColumn.setSortable(false);
        pageColumn.setPrefWidth(50);
        pageColumn.setMinWidth(50);
        pageColumn.setMaxWidth(50);
        ObservableList<TreeTableColumn<TreeTableBookmarkItem, ?>> columns = treeTableView.getColumns();
        columns.add(titleColumn);
        columns.add(pageColumn);
        treeTableView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            log.debug("select changed: {} {} {}", observable, oldValue, newValue);
            if (newValue != null) {
                titleInput.setText(newValue.getValue().getTitle());
                pageInput.setText(String.valueOf(newValue.getValue().getPage()));
            } else {
                titleInput.setText("");
                pageInput.setText("");
            }
        });
        treeTableView.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY);
        treeTableView.getRoot().addEventHandler(TreeItem.TreeModificationEvent.ANY, event -> updateJson());
    }

    private void updateJson() {
        bookmark = new Bookmark();
        for (TreeItem<TreeTableBookmarkItem> child : treeTableView.getRoot().getChildren()) {
            bookmark.getBookmarkItems().add(Bookmark.Item.ofTreeItem(child));
        }
        jsonTextArea.setText(Jsons.toJsonPretty(bookmark.getBookmarkItems()));
    }

    private void initList() {
        listView.setCellFactory(param -> new ListCell<FileListItem>() {
            @Override
            protected void updateItem(FileListItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.getName());
                }
            }
        });
    }

    //region bookmark

    public void onOpenButtonAction(ActionEvent actionEvent) {
        choosePdfFile().ifPresent(file -> openFile(file, null));
    }

    private Optional<File> choosePdfFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(__("Select a PDF file"));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(__("PDF Files"), "*.pdf"));
        return Optional.ofNullable(fileChooser.showOpenDialog(stage));
    }

    private Optional<List<File>> choosePdfFiles() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(__("Select a PDF file"));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(__("PDF Files"), "*.pdf"));
        return Optional.ofNullable(fileChooser.showOpenMultipleDialog(stage));
    }

    private void openPdfFile(File file, BiConsumer<PdfReader, byte[]> onSuccess,
            Consumer<byte[]> onException) {
        openPdfFile(file, null, onSuccess, onException);
    }

    private void openPdfFile(File file, byte[] pass, BiConsumer<PdfReader, byte[]> onSuccess,
            Consumer<byte[]> onException) {
        try {
            String filePath = file.getAbsolutePath();
            PdfReader pdfReader = new PdfReader(filePath, pass);
            onSuccess.accept(pdfReader, pass);
            pdfReader.close();
        } catch (BadPasswordException e) {
            onException.accept(pass);
            // https://stackoverflow.com/a/53825771
            Dialog<String> dialog = new Dialog<>();
            PasswordField passwordField = new PasswordField();
            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setMaxWidth(Double.MAX_VALUE);
            grid.setAlignment(Pos.CENTER_LEFT);
            grid.add(new Text(file.getName()), 0, 0, 2, 1);
            grid.add(new Label(__("Input password")), 0, 1);
            grid.add(passwordField, 1, 1);
            dialog.getDialogPane().setContent(grid);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            dialog.setResultConverter(btn -> {
                if (btn == ButtonType.OK) {
                    return passwordField.getText();
                }
                return null;
            });
            Platform.runLater(passwordField::requestFocus);
            dialog.showAndWait().ifPresent(s -> openPdfFile(file, s.getBytes(), onSuccess, onException));
        } catch (Exception e) {
            FxUtil.showAlertWithException(_f("Can not open file: {0}", file.getAbsolutePath()), e);
            onException.accept(pass);
        }
    }

    private void openFile(File file, byte[] pass) {
        openPdfFile(file, pass, (reader, password) -> {
            bookmark = PdfUtil.getBookmark(reader);
            if (bookmark != null) {
                FxUtil.showAlert(
                        __("The pdf file already has bookmarks, if you continue to edit and saved to a new file, you may lost some bookmark infos(such as font style: bold, italic) on the new file."));
                buildTree(bookmark);
                fileName.setText(file.getName());
                fileName.setTooltip(new Tooltip(file.getAbsolutePath()));
                bookmarkSrcFile = file;
                bookmarkFilePass = pass;
            }
            openFileStatus(false, password != null);
        }, password -> {
            bookmarkFilePass = null;
            fileName.setText(FILENAME_TIP);
            fileName.setTooltip(null);
            openFileStatus(true, false);
        });
    }

    private void openFileStatus(boolean exception, boolean pass) {
        if (exception) {
            statusLabel.setText(__("Open pdf file error."));
            return;
        }
        if (pass) {
            statusLabel.setText(__("Pdf file opened(with password)."));
        } else {
            statusLabel.setText(__("Pdf file opened."));
        }
    }

    private void buildTree(Bookmark bookmark) {
        TreeItem<TreeTableBookmarkItem> root = treeTableView.getRoot();
        root.getChildren().clear();
        for (Bookmark.Item bookmarkItem : bookmark.getBookmarkItems()) {
            root.getChildren().add(bookmarkItem.toTreeItem());
        }
    }

    public void onAddButtonAction(ActionEvent actionEvent) {
        TreeTableBookmarkItem item = inputItem();
        Optional<TreeItem<TreeTableBookmarkItem>> select = selectItem();
        if (select.isPresent()) {
            TreeItem<TreeTableBookmarkItem> selectItem = select.get();
            ObservableList<TreeItem<TreeTableBookmarkItem>> list = selectItem
                    .getParent()
                    .getChildren();
            list.add(list.indexOf(selectItem) + 1, new TreeItem<>(item));
        } else {
            treeTableView.getRoot().getChildren().add(new TreeItem<>(item));
        }
        statusLabel.setText(_f("Added a item. Title: {0} Page: {1}", item.getTitle(), item.getPage()));
    }

    private TreeTableBookmarkItem inputItem() {
        String title = titleInput.getText();
        String page = pageInput.getText();
        int pageIndex = Strings.parseInt(page, 0);
        return new TreeTableBookmarkItem(title, pageIndex);
    }

    public void onAddChildButtonAction(ActionEvent actionEvent) {
        TreeTableBookmarkItem item = inputItem();
        selectItem().ifPresent(select -> {
            select.getChildren().add(new TreeItem<>(item));
            select.setExpanded(true);
            statusLabel.setText(_f("Added a child item. Title: {0} Page: {1}", item.getTitle(), item.getPage()));
        });
    }

    private Optional<TreeItem<TreeTableBookmarkItem>> selectItem() {
        return Optional.of(treeTableView).map(TreeTableView::getSelectionModel)
                .map(SelectionModel::getSelectedItem);
    }

    public void onUpdateButtonAction(ActionEvent actionEvent) {
        TreeTableBookmarkItem input = inputItem();
        selectItem().ifPresent(item -> {
            item.getValue().setTitle(input.getTitle());
            item.getValue().setPage(input.getPage());
            updateJson();
            statusLabel.setText(_f("Update a item. Title: {0} Page: {1}", input.getTitle(), input.getPage()));
            treeTableView.refresh();
        });
    }

    public void onDeleteButtonAction(ActionEvent actionEvent) {
        selectItem().ifPresent(select -> {
            TreeTableBookmarkItem delete = select.getValue();
            select.getParent().getChildren().remove(select);
            statusLabel.setText(_f("Delete a item. Title: {0} Page: {1}", delete.getTitle(), delete.getPage()));
            if (treeTableView.getRoot().getChildren().isEmpty()) {
                treeTableView.getSelectionModel().clearSelection();
            }
        });
    }

    public void onSaveButtonAction(ActionEvent actionEvent) {
        if (bookmark == null) {
            statusLabel.setText(__("No bookmarks to save."));
            return;
        }
        if (bookmarkSrcFile == null) {
            statusLabel.setText(__("You should open a pdf file first."));
            return;
        }
        File file = chooseSaveFile(bookmarkSrcFile, __("_WithBookmark"));
        if (file != null) {
            if (file.equals(bookmarkSrcFile)) {
                FxUtil.showAlert(__("Save failed. You should save as a new file."));
                return;
            }
            PdfUtil.addBookmark(bookmarkSrcFile.getAbsolutePath(), bookmarkFilePass, bookmark, file.getAbsolutePath());
            statusLabel.setText(_f("Saved success as {0}.", file.getAbsolutePath()));
        } else {
            statusLabel.setText(__("Cancel save."));
        }
    }

    private File chooseSaveFile(File src, String renameSuffix) {
        String srcName = src.getName();
        FileChooser chooser = new FileChooser();
        chooser.setTitle(__("Save as"));
        chooser.setInitialDirectory(src.getParentFile());
        chooser.setInitialFileName(srcName.substring(0, srcName.length() - ".pdf".length()) + renameSuffix + ".pdf");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(__("PDF Files"), "*.pdf"));
        return chooser.showSaveDialog(stage);
    }

    public void onResetButtonAction(ActionEvent actionEvent) {
        if (bookmarkSrcFile != null) {
            openFile(bookmarkSrcFile, bookmarkFilePass);
        }
    }

    public void onTransButtonAction(ActionEvent actionEvent) {
        String json = jsonTextArea.getText();
        List<Bookmark.Item> bookmarks;
        if (json == null || json.trim().isEmpty()) {
            bookmarks = null;
        } else {
            bookmarks = Jsons.fromJson(json, new TypeReference<List<Bookmark.Item>>() {
            });
        }
        if (bookmarks != null) {
            bookmark = new Bookmark();
            bookmark.getBookmarkItems().addAll(bookmarks);
            buildTree(bookmark);
            statusLabel.setText(__("Success build bookmarks from json of text area."));
        } else {
            statusLabel.setText(__("Trans bookmarks from json failed."));
        }
    }
    //endregion bookmark

    //region merge
    public void onAddFile(ActionEvent actionEvent) {
        choosePdfFiles().ifPresent(list -> list.stream()
                .sorted(Comparator.comparing(File::getAbsolutePath))
                .forEach(file -> {
                    FileListItem item = FileListItem.fromFile(file);
                    if (!listView.getItems().contains(item)) {
                        openPdfFile(file, (reader, pass) -> {
                            item.setPass(pass);
                            listView.getItems().add(item);
                            statusLabel.setText(__("Items added."));
                        }, errorPass -> {
                        });
                    }
                }));
    }

    public void onRemoveFile(ActionEvent actionEvent) {
        Optional.of(listView)
                .map(ListView::getSelectionModel)
                .map(MultipleSelectionModel::getSelectedItems)
                .ifPresent(selected -> {
                    listView.getItems().removeAll(selected);
                    listView.refresh();
                    statusLabel.setText(__("Items removed."));
                });
    }

    public void onMoveAction(ActionEvent actionEvent) {
        Optional.of(listView)
                .map(ListView::getSelectionModel)
                .map(SelectionModel::getSelectedIndex)
                .filter(i -> i >= 0).ifPresent(index -> {
            Object source = actionEvent.getSource();
            ObservableList<FileListItem> items = listView.getItems();
            FileListItem select = items.get(index);
            if (source == moveTop && index > 0) {
                items.remove(select);
                items.add(0, select);
            }
            if (source == moveUp && index > 0) {
                items.remove(select);
                items.add(index - 1, select);
            }
            if (source == moveDown && index < items.size() - 1) {
                items.remove(select);
                items.add(index + 1, select);
            }
            if (source == moveBottom && index < items.size() - 1) {
                items.remove(select);
                items.add(select);
            }
            listView.getSelectionModel().select(select);
        });
    }

    public void onMergeAction(ActionEvent actionEvent) {
        if (!listView.getItems().isEmpty()) {
            File outFile = chooseSaveFile(listView.getItems().get(0).toFile(), __("_Merged"));
            try {
                PdfUtil.merge(outFile, listView.getItems());
                statusLabel.setText(_f("Merged to {0}.", outFile.getAbsolutePath()));
            } catch (Exception e) {
                FxUtil.showAlertWithException(__("Merge pdf files error."), e);
            }
        }
    }
    //endregion merge

    //region password

    public void onSelectSrcButtonAction(ActionEvent actionEvent) {
        choosePdfFile().ifPresent(file -> openPdfFile(file,
                (reader, pass) -> {
                    srcFile.setText(file.getName());
                    srcFile.setTooltip(new Tooltip(file.getAbsolutePath()));
                    passwordField.setText(pass == null ? "" : new String(pass));
                    passwordSrcFile = file;
                    passwordFileOldPass = pass;
                    openFileStatus(false, pass != null);
                },
                pass -> {
                    srcFile.setText(FILENAME_TIP);
                    srcFile.setTooltip(null);
                    passwordField.setText("");
                    passwordFileOldPass = null;
                    openFileStatus(true, pass != null);
                }
        ));
    }

    public void onUpdatePasswordButtonAction(ActionEvent actionEvent) {
        if (passwordSrcFile == null) {
            statusLabel.setText(__("No opened pdf file."));
            return;
        }
        String password = passwordField.getText();
        log.info("update pass. file={}, pass={}", passwordSrcFile, password);
        try {
            PdfReader pdfReader = new PdfReader(passwordSrcFile.getAbsolutePath(), passwordFileOldPass);
            if (password == null || password.isEmpty()) {
                File saveFile = chooseSaveFile(passwordSrcFile, __("_NoPassword"));
                if (saveFile != null) {
                    PdfUtil.removePassword(pdfReader, saveFile.getAbsolutePath());
                }
            } else {
                File saveFile = chooseSaveFile(passwordSrcFile, __("_WithPassword"));
                PdfUtil.addPassword(pdfReader, password.getBytes(), saveFile.getAbsolutePath());
            }
        } catch (Exception e) {
            FxUtil.showAlertWithException(__("Update password error."), e);
        }
    }
    //endregion password

    public void onGithubLinkAction(ActionEvent actionEvent) {
        app.getHostServices().showDocument(((Hyperlink) actionEvent.getSource()).getText());
    }

}