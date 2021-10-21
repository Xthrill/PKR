/*
 *   Copyright 2020-2021 Moros <https://github.com/PrimordialMoros>
 *
 *    This file is part of Bending.
 *
 *   Bending is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Bending is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with Bending.  If not, see <https://www.gnu.org/licenses/>.
 *  @author Xthrill
 */

package me.moros.bending.ability.fire;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
//import java.util.stream.Collectors;

import me.moros.atlas.configurate.CommentedConfigurationNode;
import me.moros.bending.Bending;
import me.moros.bending.ability.common.basic.ParticleStream;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.AbilityInitializer;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.Collision;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.methods.EntityMethods;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;

public class FireBall extends AbilityInstance
{
    // every ability needs a config, this will be the config object for Fireball
    private static final Config config = new Config();
    // here we declare a user, userconfig, and removalPolicy 
    private User user;
    private Config userConfig;
    private RemovalPolicy removalPolicy;
    // here we declare a firesteam which is a particle stream of fire particles (more detail on this later)
    private FireStream stream;
    
    /*
    * public Fireball Constructor passing in its description
    */ 
    public FireBall(@NonNull AbilityDescription desc)
    {
        super(desc);
    }

    /*
    * Every ability needs an activate method.
    * It instantiates and begins the ability.
    * So basically when the ability is (activate)d in this case with a left click
    * this method is called and begins processing what to do to begin making the FireBall
    * 
    * @param User - the user information, basically who activated the ability and have their info ready for use
    * @param Activation - FIXME : not needed
    */
    @Override
    public boolean activate(@NonNull User user, @NonNull Activation method)
    {
        this.user = user;
        loadConfig(); // defined below

        // if the user is in water, do not activate the ability.
        if (user.mainHandSide().toBlock(user.world()).isLiquid())
        {
            return false; // returns that the ability wasn't activated.
        }

        launch(); // in charge of creating the ability. defined below.

        removalPolicy = Policies.builder() // sets the policies for removing the ability
            .add(Policies.IN_LIQUID) // the ability touches aqua
            .build(); // uh, idk, but it needs it

        return true; // returns that the ability was activated.
    }

    /*
    * Every ability needs a config overload.
    * This method calls the Bending.configManager to update the userConfig.
    * The Bending.configManager, assuming, takes the info from the "bending.conf" file
    * and updates the format to a "Config" datatype and returns it
    */
    @Override
    public void loadConfig()
    {
        userConfig = Bending.configManager().calculate(this, config);
    }

    /*
    * Every ability needs an update method.
    * This method is constantly being run when the ability object is "alive".
    * Basically after the activate method, this update method is then called over and over.
    * the stream.update() method call determines when to stop iterating over 
    *   this update() method for fireball.
    * if stream.update() returns UpdateResult.CONTINUE enum value then continue running the update method.
    * else if it returns UpdateResult.REMOVE then stop running it.
    */
    @Override
    public @NonNull UpdateResult update()
    {
        // notice that we also include that if the removalPolicy.test() returns true
        // then we will automatically return UpdateResult.REMOVE which will stop the update method
        // from being iterated over again.
        // The removalPolicy object determines when the fireball should be destroyed, for example
        // if the fireball comes in contact with a block, it should be destroyed. In other words,
        // The removalPolicy.test() checks for when the ability should be destroyed.
        if (removalPolicy.test(user, description()))
        {
            return UpdateResult.REMOVE;
        }
        
        return stream.update();
    }

    /*
    * Here we create the FireStream.
    * And we set the ability cooldown.
    */
    private void launch()
    {
        removalPolicy = Policies.builder().build(); // FIXME, was already defined in activate()
        user.addCooldown(description(), userConfig.cooldown); // adds cooldown to the ability.
        Vector3d origin = user.mainHandSide(); // origin, a 3d coordinate, is at user's hand side.
        Vector3d lookingDir = user.direction().multiply(userConfig.range); // lookingdir, a 3d normalized or unit vector,
            // set as the direction that the user is looking. 
            // Probably calculated using the back of the users head with the front, or something like that.
        // creates a particle stream object of FireStream, or stream of fire particles.
        // this is the heart of the abilities existence.
        // This variable will be used later in the code for specifying the look and feel and the way it should
        //  behave.
        // FireStream constructor takes in a Ray
        //  which is constructed by taking in a 
        //  starting location as well as the direction of where the ability will go.
        stream = new FireStream(new Ray(origin, lookingDir));
    }

    /*
    * Every Ability needs a colliders method
    * 
    * @returns a Collection of colliders.
    *  if stream is null then return an empty "List"
    *  otherwise return the stream colliders
    */ 
    @Override
    public @NonNull Collection<@NonNull Collider> colliders()
    {
        return stream == null ? List.of() : List.of(stream.collider());
    }

    /*
    * You get the idea by now, every method that has @Override, it GENERALLY (not always) means it 
    *  is required by the Ability interface which we implemented at the top of this class.
    *  If you don't remember what an interface is, I recommend going back tutorialspoint for java.
    * This method is called when a collision happens, here we handle what should happen when that
    *  collision occurs.
    * @param collision - the collision object which can give us more info on the collision.
    */ 
    @Override
    public void onCollision(@NonNull Collision collision)
    {
        Ability collidedAbility = collision.collidedAbility(); // Here we get the other ability in
            // which the fireball collided with
        if (collision.removeSelf()) // yes - in other words, idk. Probably if the collision is a type where
            // our ability or the other ability that was collided with is removable or should be removed?
        {
            String name = collidedAbility.description().name(); // get the name of the other ability
            if (AbilityInitializer.layer3.contains(name)) // if the other ability is of type layer3
                // or the list of abilities in which fireball has dominance over then
                // castaway that puny other ability!
            {
                collision.removeOther(true);
            }
            else // otherwise remove, aka shit on fireball 
            {
                collision.removeSelf(false);
            }
        }
    }
    
    // return the user. period.
    @Override
    public @MonotonicNonNull User user()
    {
        return user;
    }

    /*
    * OOooo a nested class - a class within a class, fun...
    * Anyways, This is the FireStream class we've been talking about for the previous code above.
    * This class defines what and how the particles will be displayed to us as well as other methods.
    */
    private class FireStream extends ParticleStream // Recap: FireStream is a child class of ParticleStream
    {

        /*
        * FireStream's constructor
        * @param ray - ray contains the origin and looking-direction vectors
        */
        public FireStream(Ray ray)
        {
            super(user, ray, userConfig.speed, 1.0); // ParticleStream's param needs more than a ray
                // it takes in a user, ray, the speed of the ability, and a uh factor number thingy.
            canCollide = Block::isLiquid; // yes
        }

        /*
        * Here we Override, this time it is not required to override bc we don't have an
        * interface nagging at us that we need to define a certain method.
        *
        * But we will override ParticleStream's render() and make our own instead of using the parent's version of it.
        * The render method renders our ability (how will it look).
        * 
        * Also know that when we update() our FireBall, the stream.render is called, somewhere else...
        * In other words, render is continuously being called while the ability is alive.
        */
        @Override
        public void render()
        {
            Location loc = bukkitLocation(); // get the location? not exactly sure how this works
            ParticleUtil.createFire(user, loc) // Using the user's info and location, we create a fire particle at 
                .extra(0.01).spawn(); // that location with an extra (not needed) random displacement/offset of the particle
                // spawn simply spawns the particle :)
        }
        
        /*
        * Every time render is called, so is postRender right after.
        * In this method we play the fire sound woooshhh!
        * Technically we could put more in this method, but one sound is enough for fireball.
        */
        @Override
        public void postRender() {
            if (ThreadLocalRandom.current().nextInt(3) == 0) // yes... probably randomly 
                // determines if the sound should be played or not. out of 3 numbers, if 0 play sound
            {
                SoundUtil.FIRE.play(bukkitLocation());
            }
        }

        /*
        * This is method is called when our FireStream object collides with an entity.
        *  Here we apply damage to the entity
        *  And we also apply the knockback.
        * @param entity
        */
        @Override
        public boolean onEntityHit(@NonNull Entity entity)
        {
            DamageUtil.damageEntity(entity, user, userConfig.damage, description());
            EntityMethods.applyVelocity(FireBall.this, entity, ray.direction.normalize().multiply(0.5));
            return true;
        }

        /*
        * YES!!!
        * @param block
        * @return true
        */
        @Override
        public boolean onBlockHit(@NonNull Block block)
        {
            return true;
        }

        // @return location
        private @NonNull Vector3d location()
        {
            return location;
        }
    }

    /*
    * o.m.g, another nested class. 
    * Here we do all the config stuff for Fireball
    */
    private static class Config extends Configurable // Configurable is Config's daddy
    {
        // the config variables, self explanatory
        @Modifiable(Attribute.COOLDOWN) // modifiable probably means that u can change it :) in this case the Attribute enum thingy
        public long cooldown;
        @Modifiable(Attribute.DAMAGE)
        public double damage;
        @Modifiable(Attribute.RANGE)
        public double range;
        @Modifiable(Attribute.SPEED)
        public double speed;
        @Modifiable(Attribute.RADIUS)
        public double radius;

        /*
        * When we reload the config after making our goodie changes, we call this method to apply those changes
        */
        @Override
        public void onConfigReload()
        {
            CommentedConfigurationNode abilityNode = config.node("abilities", "fire", "fireball"); // this object does the
                // magic of reading and retrieving the data from "bending.conf"
                // oh and other stuff

            cooldown = abilityNode.node("cooldown").getLong(1500); // these numbers are the default in case
            damage = abilityNode.node("damage").getDouble(2.0); // we don't have a value in the file.
            range = abilityNode.node("range").getDouble(18.0);
            speed = abilityNode.node("speed").getDouble(0.8);
            radius = abilityNode.node("radius").getDouble(1.5);
        }
    }
}
