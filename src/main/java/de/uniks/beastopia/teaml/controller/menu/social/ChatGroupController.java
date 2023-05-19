package de.uniks.beastopia.teaml.controller.menu.social;

import de.uniks.beastopia.teaml.Main;
import de.uniks.beastopia.teaml.controller.Controller;
import de.uniks.beastopia.teaml.rest.Group;
import de.uniks.beastopia.teaml.service.GroupListService;
import de.uniks.beastopia.teaml.service.TokenStorage;
import de.uniks.beastopia.teaml.utils.Dialog;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;

import javax.inject.Inject;
import javax.inject.Provider;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

public class ChatGroupController extends Controller {

    @FXML
    HBox _rootElement;
    @FXML
    Button pinGroupBtn;
    @FXML
    Button deleteGroupBtn;
    @FXML
    Button editGroupBtn;
    @FXML
    Text name;

    private Group group;
    private Boolean pinned;
    private ImageView pinnedImg;
    private ImageView notPinnedImg;
    //private ImageView abort;

    @Inject
    Preferences preferences;
    @Inject
    GroupListService groupListService;
    @Inject
    Provider<DirectMessageController> directMessageControllerProvider;
    @Inject
    TokenStorage tokenStorage;
    private final Consumer<Group> onPinChanged = null;
    private Consumer<Group> onGroupClicked = null;

    @Inject
    public ChatGroupController() {

    }

    @Override
    public void init() {
        try {
            pinnedImg = createImage(Objects.requireNonNull(Main.class.getResource("assets/buttons/filled_pin.png")));
            notPinnedImg = createImage(Objects.requireNonNull(Main.class.getResource("assets/buttons/pin.png")));
        } catch (URISyntaxException | FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void setOnGroupClicked(Consumer<Group> onGroupClicked) {
        this.onGroupClicked = onGroupClicked;
    }

    @SuppressWarnings("UnusedReturnValue")
    public ChatGroupController setGroup(Group group, boolean pinned) {
        this.group = group;
        this.pinned = pinned;
        return this;
    }

    @Override
    public Parent render() {
        Parent parent = super.render();
        name.setText(group.name());

        if (this.pinned) {
            this.pinGroupBtn.setGraphic(pinnedImg);
        } else {
            this.pinGroupBtn.setGraphic(notPinnedImg);
        }

        return parent;
    }

    public void mouseClicked() {
        onGroupClicked.accept(group);
    }

    private ImageView createImage(URL imageUrl) throws URISyntaxException, FileNotFoundException {
        ImageView imageView = new ImageView(loadImage(imageUrl));
        imageView.setFitHeight(25.0);
        imageView.setFitWidth(25.0);
        return imageView;
    }

    private static Image loadImage(URL imageUrl) throws FileNotFoundException, URISyntaxException {
        return new Image(new FileInputStream(new File(imageUrl.toURI())));
    }

    public void editGroup() {
        //TODO: show edit group dialog
    }

    @FXML
    public void deleteGroup() {
        if (group.members().size() < 2) {
            disposables.add(groupListService.deleteGroup(group).observeOn(FX_SCHEDULER).subscribe(
                    lr -> app.show(directMessageControllerProvider.get()),
                    error -> Dialog.error(error, resources.getString("deleteFailed")
                    )));
        } else {
            disposables.add(groupListService.removeMember(group, tokenStorage.getCurrentUser()._id()).observeOn(FX_SCHEDULER).subscribe(
                    lr -> app.show(directMessageControllerProvider.get()),
                    error -> Dialog.error(error, resources.getString("deleteFailed")
                    )));
        }
    }

    //TODO: implement pinning feature
    public void pinGroup() {
        if (pinGroupBtn.getGraphic() == notPinnedImg) {
            pinGroupBtn.setGraphic(pinnedImg);
            preferences.putBoolean(this.group._id() + "_pinned", true);
        } else {
            pinGroupBtn.setGraphic(notPinnedImg);
            preferences.putBoolean(this.group._id() + "_pinned", false);
        }
        //noinspection ConstantValue
        if (onPinChanged != null) {
            onPinChanged.accept(group);
        }
    }
}
