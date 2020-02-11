package controllers;

import actions.TimerAction;
import com.fasterxml.jackson.databind.JsonNode;
import models.Ingredient;
import models.NutritionalInformation;
import models.Recipe;
import play.data.Form;
import play.data.FormFactory;
import play.libs.Json;
import play.mvc.*;

import javax.inject.Inject;
import java.util.List;

import static play.libs.Json.toJson;

public class RecipeController extends Controller {


    @Inject
    FormFactory formFactory;


    @With(TimerAction.class)
    public Result createRecipe(Http.Request request){


        Form<Recipe> form = formFactory.form(Recipe.class).bindFromRequest(request);

        if(form.hasErrors()){
            return Results.badRequest(form.errorsAsJson());
        }

        Recipe recipe = form.get();

        NutritionalInformation nutritionalInformation = recipe.getNutritionalInformation();

        if (Recipe.findByName(recipe.getName()) != null) {
            return Results.status(403, "Error: Duplicated Recipe");
        }

        nutritionalInformation.setRecipe(recipe);

        nutritionalInformation.save();

        recipe.save();


        if(request.accepts("application/json"))
        {
            JsonNode json = Json.toJson(recipe);
            return Results.ok(json);
        } else if(request.accepts("application/xml"))
        {
           return Results.ok(views.xml.recipe.render(recipe));
        } else
        {
            return Results.status(415);
        }
    }


    @With(TimerAction.class)
    public Result addIngredientToRecipe(Http.Request request, String nameIngredient, String nameRecipe)
    {
        Ingredient ingredient = Ingredient.findByName(nameIngredient);
        Recipe recipe = Recipe.findByName(nameRecipe);

        if(ingredient == null)
        {
            return Results.status(404, "Error: Ingredient not found");
        }
        else if(recipe == null)
        {
            return Results.status(404, "Error: Recipe not found");
        }
        recipe.addIngredient(ingredient);
        ingredient.addRecipe(recipe);

        recipe.update();
        ingredient.update();

        if(request.accepts("application/json"))
        {
            JsonNode json = Json.toJson(recipe);
            return Results.ok(json);
        } else if(request.accepts("application/xml"))
        {
            return Results.ok(views.xml.recipe.render(recipe));
        } else
        {
            return Results.status(415);
        }

    }



    public Result findRecipeByName(Http.Request request, String name)
    {
        Recipe recipe = Recipe.findByName(name);
        if(recipe == null)
        {
            return Results.status(404, "Error: Recipe Not Found");
        }
        else if(request.accepts("application/json"))
        {
            JsonNode json = toJson(recipe);
            return Results.ok(json);
        } else if(request.accepts("application/xml"))
        {
            return Results.ok(views.xml.recipe.render(recipe));
        } else
        {
            return Results.status(415);
        }
    }


    @With(TimerAction.class)
    public Result listVegetarianRecipes(Http.Request request)
    {
        List<Recipe> recipes = Recipe.listVegetarian();

        if(recipes == null)
        {
            return Results.status(404, "Error: Vegetarian Recipes Not Found");
        } else if(request.accepts("application/json"))
        {
            JsonNode json = toJson(recipes);
            return Results.ok(json);
        } else if(request.accepts("application/xml"))
        {
            return Results.ok(views.xml.recipes.render(recipes));
        } else
        {
            return Results.status(415);
        }
    }


    @With(TimerAction.class)
    public Result updateRecipe(Http.Request request, String name) {
        // TODO Use Form
        JsonNode node = request.body().asJson();

        Form<Recipe> form = formFactory.form(Recipe.class).bindFromRequest(request);

        if(form.hasErrors()){
            return Results.badRequest(form.errorsAsJson());
        }

        Recipe recipe = Recipe.findByName(name);
        if (recipe == null)
        {
            return Results.status(404, "Error: Recipe Not Found");

        } else {
            if(node.has("name"))
                recipe.setName(node.get("name").asText());

            /*if(Recipe.findByName(recipe.getName()) != null ){
                return Results.status(403, "Error: Duplicated Recipe");
            }*/

            if(node.has("vegetarian"))
                recipe.setVegetarian(node.get("vegetarian").asBoolean());
            if(node.has("nutritionalInformation")) {
                JsonNode node2 = node.path("nutritionalInformation");

                NutritionalInformation nut = new NutritionalInformation();
                if(node2.has("grams"))
                    nut.setGrams(node2.get("grams").asInt());

                if(node2.has("calories"))
                    nut.setCalories(node2.get("calories").asInt());
                if(node2.has("cholesterol"))
                    nut.setCholesterol(node2.get("cholesterol").asDouble());

                if(node2.has("protein"))
                    nut.setProtein(node2.get("protein").asDouble());
                if(node2.has("vitamins"))
                    nut.setVitamins(node2.get("vitamins").asText());

                recipe.setNutritionalInformation(nut);

                nut.save();
            }
            recipe.update();

            if (request.accepts("application/json")) {
                JsonNode json = toJson(recipe);
                return Results.ok(json);
            } else if (request.accepts("application/xml")) {
                return Results.ok(views.xml.recipe.render(recipe));
            } else {
                return Results.status(415);
            }
        }
    }



    @play.db.ebean.Transactional
    public Result deleteRecipe(String name)
    {
        Recipe recipe = Recipe.findByName(name);
        if (recipe == null)
        {
            return Results.status(404, "Error: Recipe Not Found");

        } else{
            recipe.refresh();
            Recipe.deleteRecipeWithDependencies(recipe);
            return Results.ok("The recipe "+ name + " was deleted");
        }

    }



}
