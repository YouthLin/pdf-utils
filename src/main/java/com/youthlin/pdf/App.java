package com.youthlin.pdf;

import com.youthlin.pdf.controller.MainLayout;
import com.youthlin.utils.i18n.Translation;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import static com.youthlin.utils.i18n.Translation.__;

/**
 * @author youthlin.chen
 * @date 2019-10-12 15:25
 */
public class App extends Application {

    public static void main(String[] args) {
        Translation.setDft(Translation.getBundle("Message"));
        launch(App.class, args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle(__("PDF Utils"));
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/layout.fxml"));
        Parent root = fxmlLoader.load();
        MainLayout mainLayout = fxmlLoader.getController();
        mainLayout.stage = primaryStage;
        mainLayout.app = this;
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.show();
        primaryStage.setMinWidth(primaryStage.getWidth());
        primaryStage.setMinHeight(primaryStage.getHeight());
    }

}
