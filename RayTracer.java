//import classes we will use
import java.util.Arrays;
import java.io.File;
import java.util.Scanner;
import java.io.FileNotFoundException;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.ArrayList;

//import the JAMA library, which will be used for Matrix operations and for constructing "vectors" 
import Jama.Matrix;

public class RayTracer {

    //define camera property variables
    static float near;
    static float left;
    static float right;
    static float top;
    static float bottom;
    //define resolution variables
    static int res_x;
    static int res_y;
    //define background colour variables
    static float background_r;
    static float background_g;
    static float background_b;
    //define ambient intensity variables
    static float ambient_r;
    static float ambient_g;
    static float ambient_b;
    //define output file variable
    static String outputfile;
    
    //Sphere object, which has the following properties:
    static class Sphere {
        //name
        String name;
        //3D position
        float x_pos;
        float y_pos;
        float z_pos;
        //scale
        float scl_x;
        float scl_y;
        float scl_z;
        //colour
        float r;
        float g;
        float b;
        //ambient, diffuse, specular, and reflection coefficients
        float ka;
        float kd;
        float ks;
        float kr;
        //specular exponent n, which will be used when calculating the specular portion of light
        int n;
        //transformation matrix, given the position and scaling factors above
        Matrix transform_matrix;
    }
    //Point light source object, which has the following properties:
    static class Light {
        //name
        String name;
        //3D position
        float x_pos;
        float y_pos;
        float z_pos;
        //light intensity
        float ir;
        float ig;
        float ib;
    }

    //Ray object, which has the following properties:
    static class Ray {
        //Starting point S
        Matrix start_point;
        //direction vector c
        Matrix direction;
        //depth value to keep track of reflection bounces
        int depth;
        //boolean value, where 1 indicates that a ray is a reflected ray
        int reflect;

        //depth setter function
        void setDepth (int setdepth) {
            this.depth = setdepth;
        }
    }

    //Intersected sphere object, which has the following properties:
    static class IntersectedObj {
        //vector normal to intersected point on sphere
        Matrix normal;
        //the intersection point on the sphere
        Matrix intersection_pnt;
        //t-value it takes to get to the intersection point
        float t;
        //the index in the sphere list where the object is located
        int index_in_sphere_list;
        //boolean value which indicates whether or not the object is clipped by the near plane
        int clipped;
    }

    //define two ArrayLists: a list of spheres in our scene, and a list of lights in our scene
    static ArrayList<Sphere> sphere_list = new ArrayList<Sphere>();
    static ArrayList<Light> light_list = new ArrayList<Light>();

    //This function will parse the input file and assign the correct values to the variables declared above
    //parameters: args, which are the input arguments we type when running the program
    //returns: nothing (void)
    public static void parse(String[] args) {
        String filename = args[0];
        File input_file = new File(filename);
        try {
            //use scanner to read the input file
            Scanner scan = new Scanner(input_file);
            while (scan.hasNext()) {
                String cur_line = scan.nextLine();
                //split each line by one or more spaces using regular expression
                String[] line_split = cur_line.split("\\s+");

                if (line_split[0].equals("NEAR")) {
                    near = Float.parseFloat(line_split[1]);
                }
                if (line_split[0].equals("LEFT")){
                    left = Float.parseFloat(line_split[1]);
                }
                if (line_split[0].equals("RIGHT")){
                    right = Float.parseFloat(line_split[1]);
                }
                if (line_split[0].equals("BOTTOM")){
                    bottom = Float.parseFloat(line_split[1]);
                }
                if (line_split[0].equals("TOP")){
                    top = Float.parseFloat(line_split[1]);
                }
                if (line_split[0].equals("RES")){
                    res_x = Integer.parseInt(line_split[1]);
                    res_y = Integer.parseInt(line_split[2]);
                }
                if (line_split[0].equals("SPHERE")){
                    Sphere cur_sphere = new Sphere();
                    cur_sphere.name = line_split[1];
                    cur_sphere.x_pos = Float.parseFloat(line_split[2]);
                    cur_sphere.y_pos = Float.parseFloat(line_split[3]);
                    cur_sphere.z_pos = Float.parseFloat(line_split[4]);
                    cur_sphere.scl_x = Float.parseFloat(line_split[5]);
                    cur_sphere.scl_y = Float.parseFloat(line_split[6]);
                    cur_sphere.scl_z = Float.parseFloat(line_split[7]);
                    cur_sphere.r = Float.parseFloat(line_split[8]);
                    cur_sphere.g = Float.parseFloat(line_split[9]);
                    cur_sphere.b = Float.parseFloat(line_split[10]);
                    cur_sphere.ka = Float.parseFloat(line_split[11]);
                    cur_sphere.kd = Float.parseFloat(line_split[12]);
                    cur_sphere.ks = Float.parseFloat(line_split[13]);
                    cur_sphere.kr = Float.parseFloat(line_split[14]);
                    cur_sphere.n = Integer.parseInt(line_split[15]);
                    sphere_list.add(cur_sphere);
                }
                if (line_split[0].equals("LIGHT")){
                    Light cur_light = new Light();
                    cur_light.name = line_split[1];
                    cur_light.x_pos = Float.parseFloat(line_split[2]);
                    cur_light.y_pos = Float.parseFloat(line_split[3]);
                    cur_light.z_pos = Float.parseFloat(line_split[4]);
                    cur_light.ir = Float.parseFloat(line_split[5]);
                    cur_light.ig = Float.parseFloat(line_split[6]);
                    cur_light.ib = Float.parseFloat(line_split[7]);
                    light_list.add(cur_light);
                }
                if (line_split[0].equals("BACK")){
                    background_r = Float.parseFloat(line_split[1]);
                    background_g = Float.parseFloat(line_split[2]);
                    background_b = Float.parseFloat(line_split[3]);
                }
                if (line_split[0].equals("AMBIENT")){
                    ambient_r = Float.parseFloat(line_split[1]);
                    ambient_g = Float.parseFloat(line_split[2]);
                    ambient_b = Float.parseFloat(line_split[3]);
                }
                if (line_split[0].equals("OUTPUT")){
                    outputfile = line_split[1];
                }
            }
        } catch (FileNotFoundException error) {
            System.out.println("File not found.");
        }
    }

    //This function computes the dot product between two vectors of equal dimension
    //parameters: m1, the first vector, and m2, the second vector
    //returns: computed dot product (double)
    public static double dot_product(Matrix m1, Matrix m2) {
        double product = m1.get(0, 0) * m2.get(0, 0) + m1.get(1, 0) * m2.get(1, 0) + m1.get(2, 0) * m2.get(2, 0);
        return product;
    }
   
    //This function will find the closest intersected object when we shoot ray in a particular direction
    //parameters: ray, which is the Ray object that we will shoot and check if it intersects any object
    //returns: the closest object we intersect, or NULL if there is no intersection found (IntersectedObj)
    public static IntersectedObj find_closest_intersection_ray(Ray ray) {
        //set the closest_t value to +infinity
        float closest_t = Float.POSITIVE_INFINITY;
        //initialize intersected object as NULL
        IntersectedObj cur_obj = null;

        //loop through every sphere in the sphere_list and check if our ray intersects any of them
        for (int i = 0; i < sphere_list.size(); i++) {
            //construct the inverse transform matrix that we will apply to our ray
            Matrix inverse_transform = sphere_list.get(i).transform_matrix.inverse();

            //convert start point of ray into homogeneous coordinates to perform matrix multiplication
            Matrix homogeneous_start_point = new Matrix(new double[]{ray.start_point.get(0, 0), ray.start_point.get(1, 0), ray.start_point.get(2, 0), 1}, 4);
            //convert direction of ray into homogeneous coordinates to perform matrix multiplication
            Matrix homogeneous_direction = new Matrix(new double[]{ray.direction.get(0, 0), ray.direction.get(1, 0), ray.direction.get(2, 0), 0}, 4);
            
            //We now compute inverse-transformed ray S' + c't
            Matrix start_point_prime_homogeneous = inverse_transform.times(homogeneous_start_point);
            Matrix direction_prime_homogeneous = inverse_transform.times(homogeneous_direction);

            //convert back to cartesian so we can plug into quadratic formula
            Matrix S_prime = new Matrix(new double[]{start_point_prime_homogeneous.get(0, 0), start_point_prime_homogeneous.get(1, 0), start_point_prime_homogeneous.get(2, 0)}, 3);
            Matrix c_prime = new Matrix(new double[]{direction_prime_homogeneous.get(0, 0), direction_prime_homogeneous.get(1, 0), direction_prime_homogeneous.get(2, 0)}, 3);

            //compute discriminant B^2 - AC
            float discriminant = ((float)Math.pow(dot_product(S_prime, c_prime), 2)) - (((float)Math.pow(c_prime.norm2(), 2)) * ((float)Math.pow(S_prime.norm2(), 2) - 1));

            if (discriminant <= 0.0) {
                continue;
            }

            //if we get here, it means there are two solutions. So, we solve for t1 and t2.
            float t1 = -(((float)dot_product(S_prime, c_prime))/((float)Math.pow(c_prime.norm2(), 2))) - ((float)Math.sqrt(discriminant) / (float)Math.pow(c_prime.norm2(), 2));
            float t2 = -(((float)dot_product(S_prime, c_prime))/((float)Math.pow(c_prime.norm2(), 2))) + ((float)Math.sqrt(discriminant) / (float)Math.pow(c_prime.norm2(), 2));

            float smaller_t;
            int clip = 0;
            
            //take t1 as the smaller t unless t1 doesn't go past the near plane
            if (t1<=t2 && t1> 0) {
                smaller_t = t1;
            } else if (t1<near && t2 > 0) {
                smaller_t = t2;
            } else {
                continue;
            }
            //if the smaller t we get from the current sphere is smaller than the "global" minimum t-value, then set smaller_t to be the new minimum t-value
            if (smaller_t < closest_t) {
            
                closest_t = smaller_t;
                //if cur_obj is null, create new object of type IntersectedObj
                if (cur_obj == null) {
                    cur_obj = new IntersectedObj();
                }
                //set the properties of cur_obj, including the intersection_pnt
                cur_obj.index_in_sphere_list = i;
                cur_obj.t = closest_t;
                cur_obj.intersection_pnt = ray.start_point.plus(ray.direction.times((double)cur_obj.t));
                //if the z-value of the intersected point is between the origin and near plane, set clip to 1
                if (cur_obj.intersection_pnt.get(2,0) > -near) {
                    clip = 1;
                }
                cur_obj.clipped = clip;
                
                //compute normal transformation
                Matrix normal_prime = S_prime.plus(c_prime.times(closest_t));
                normal_prime = normal_prime.times(1.0/normal_prime.norm2()); //normalize

                //convert normal into homogeneous coordinate system so we can apply inverse transpose transform matrix to normal
                Matrix normal_homogeneous = new Matrix(new double[]{normal_prime.get(0,0), normal_prime.get(1,0), normal_prime.get(2,0),0},4);
                Matrix inverse_transpose = inverse_transform.transpose();

                //now we get the correctly-transformed normal vector
                Matrix normal_transformed =  inverse_transpose.times(normal_homogeneous);
        
                //convert normal back into cartesian space
                Matrix normal_cartesian = new Matrix(new double[]{normal_transformed.get(0,0),normal_transformed.get(1,0),normal_transformed.get(2,0)},3);

                //normalize the normal in cartesian space
                normal_cartesian = normal_cartesian.times(1.0 / normal_cartesian.norm2());

                cur_obj.normal = normal_cartesian;
            }
        }
        return cur_obj;
    }

    //This function will check if intersected point on IntersectedObj is in shadow, and apply the diffuse and specular components of ADS
    //lighting if not in shadow
    //parameters: p, which is the intersected object we are dealing with, and light, which is the light we will use to check if the intersected
    //point is in shadow or not
    //returns: the rgb colour component of shadow ray (Matrix)
    public static Matrix shadowRays(IntersectedObj p, Light light) {
        //initialize with no contribution
        Matrix shadow_contribution = new Matrix(new double[]{0,0,0}, 3);

        //light position
        Matrix light_pos = new Matrix(new double[]{light.x_pos, light.y_pos, light.z_pos}, 3);
        Matrix light_intensity = new Matrix(new double[]{light.ir, light.ig, light.ib}, 3);
        //intersected sphere colour
        Matrix sphere_colour = new Matrix(new double[]{sphere_list.get(p.index_in_sphere_list).r, sphere_list.get(p.index_in_sphere_list).g, sphere_list.get(p.index_in_sphere_list).b},3);
        //normal
        Matrix N = p.normal;

        //if the object we are dealing with is clipped by the near plane, invert the normal vector
        if(p.clipped == 1) {
            N = N.times(-1);
            N = N.times(1.0/N.norm2()); //normalize
        }

        //make new ray from intersected point to light, then normalize it.
        Ray ray_to_light = new Ray();
        ray_to_light.direction = light_pos.minus(p.intersection_pnt);
        ray_to_light.direction = ray_to_light.direction.times(1.0/ray_to_light.direction.norm2()); //normalize
        //let the start point of our new ray be the intersection point + some offset, which will help avoid self-intersections
        ray_to_light.start_point = p.intersection_pnt.plus(ray_to_light.direction.times(0.0001f));

        //find the closest intersection when we shoot the newly constructed Ray
        IntersectedObj q = find_closest_intersection_ray(ray_to_light);
        Matrix L = ray_to_light.direction;

        if ((q == null && p.clipped == 0) || (q!= null && (q.t > light_pos.minus(p.intersection_pnt).norm2()))) {
            //compute diffuse
            double N_dot_L = Math.max(0.0, dot_product(N, L));
            
            Matrix tmp1 = sphere_colour.times(N_dot_L);
            Matrix tmp2 = light_intensity.arrayTimes(tmp1);
            Matrix tmp3 = tmp2.times(sphere_list.get(p.index_in_sphere_list).kd);
            shadow_contribution = tmp3;
            
            //let's now compute specular
            //construct reflected vector R
            Matrix R = N.times(2.0*N_dot_L);
            R = R.minus(L); 
            R = R.times(1.0/R.norm2()); //normalize

            //construct vector V
            Matrix V = new Matrix(new double[]{-ray_to_light.start_point.get(0,0), -ray_to_light.start_point.get(1,0), -ray_to_light.start_point.get(2,0)}, 3);
            V = V.times(1.0/V.norm2()); //normalize  
    
            double R_dot_V = Math.max(0.0, dot_product(R, V));

            tmp1 = light_intensity.times((float)Math.pow(R_dot_V, sphere_list.get(p.index_in_sphere_list).n));
            tmp2 = tmp1.times(sphere_list.get(p.index_in_sphere_list).ks);
            
            //add specular term to diffuse
            shadow_contribution = shadow_contribution.plus(tmp2);
        }
        return shadow_contribution;
    }

    //This function will determine the colour of a point on the screen
    //parameters: ray, which is the ray being traced
    //returns: the rgb colour of a point on the screen (Matrix)
    public static Matrix raytrace(Ray ray) {
        //allow three bounces of reflected rays. If more than this, we return black
        if (ray.depth > 4) {
            return new Matrix(new double[]{0, 0, 0}, 3);
        }

        //find the closest intersection from the ray
        IntersectedObj p = find_closest_intersection_ray(ray);

        //if no intersection occurs, we check if the ray is reflected(in which case return (0,0,0)) or not (return background colour)
        if (p == null) {
            if (ray.reflect == 1) {
                return new Matrix(new double[]{0,0,0}, 3);
            } else {
                return new Matrix(new double[]{(int)background_r, (int)background_g, (int)background_b}, 3);
            }
        } else {
            //if we get here, it means there was an intersection, so we need to reflect
            ray.depth++;
            Sphere intersect_sphere = sphere_list.get(p.index_in_sphere_list);

            //ambient light
            double[] ambient = {ambient_r*intersect_sphere.r*intersect_sphere.ka, ambient_g*intersect_sphere.g*intersect_sphere.ka, ambient_b*intersect_sphere.b*intersect_sphere.ka };
            Matrix clocal = new Matrix(ambient, 3);

            //clocal = Sum(shadowRays(p, Light[i]))
            //loop through the light list and add the contributions from each light source
            for (int i = 0; i < light_list.size(); i++) {
                clocal = clocal.plus(shadowRays(p, light_list.get(i)));
            }

            //construct reflected ray
            Matrix N = p.normal;
            Matrix c = ray.direction;
            float N_dot_c = (float)dot_product(N, c);

            Matrix v = N.times(-2.0*N_dot_c);
            v = v.plus(c); 
			//normalize v
            v = v.times(1.0/v.norm2());
            
            Ray r_re = new Ray();
            r_re.depth = ray.depth;
            r_re.reflect = 1;

            r_re.direction = v;
            //add offset to starting point of reflected ray to avoid self-intersections
            r_re.start_point = p.intersection_pnt.plus(v.times(0.001f));
            //recursively call raytrace with the reflected ray
            Matrix c_re = raytrace(r_re); 
            
            return (clocal.plus(c_re.times(intersect_sphere.kr)));
        }
    }

    //This function will set up the translation transformation matrix 
    //parameters: sphere, which is the Sphere object we want to translate, and matrix, which should just be the identity matrix in main()
    //returns: the translation transformation matrix for the sphere (Matrix)
    public static Matrix translate(Sphere sphere, Matrix matrix) {
        Matrix translated = matrix.copy();
        translated.set(0, 3, sphere.x_pos);
        translated.set(1, 3, sphere.y_pos);
        translated.set(2, 3, sphere.z_pos);
        return translated;
    }

    //This function will set up the scale transformation matrix 
    //parameters: sphere, which is the Sphere object we want to translate
    //returns: the scale transformation matrix for the sphere (Matrix)
    public static Matrix scale_matrix(Sphere sphere) {
        return new Matrix(new double[][] {{sphere.scl_x, 0, 0, 0}, {0, sphere.scl_y, 0, 0}, {0, 0, sphere.scl_z, 0}, {0, 0, 0, 1}});
    }

    //This function will output the image in .ppm format (p3 version) 
    //parameters: pixels, the array of pixel colours in rgb, and k, a counter
    //returns: nothing (void)
    public static void outputImage(double[] pixels, int k){ 
        try {
            PrintWriter file_out = new PrintWriter(new FileWriter(outputfile));
            file_out.println("P3");
            file_out.println(res_x + " " + res_y);
            file_out.println("255");

            k = 0 ;
            //loop through all pixels
            for(int j = 0; j < res_y; j++) { 
                for( int i = 0 ; i < res_x; i++)
                {
                    file_out.println((int)pixels[k] + " " + (int)pixels[k + 1] + " " + (int)pixels[k + 2]);
                    k = k + 3 ;
                }
                file_out.println();
            }
            file_out.close();
        } catch (IOException error) {
            System.out.println("IOException thrown");
        }
    }

    
    //Function: main
    //Parameters: args (input arguments)
    //Returns: nothing (void)
    public static void main(String[] args) {
        float uc;
        float vr;
      
        //parse the given arguments
        parse(args);

        //initialize transformation matrices
        Matrix initial_transform = Matrix.identity(4,4);
        for (int i = 0; i < sphere_list.size(); i++) {
            sphere_list.get(i).transform_matrix = translate(sphere_list.get(i), initial_transform);
            sphere_list.get(i).transform_matrix = sphere_list.get(i).transform_matrix.times(scale_matrix(sphere_list.get(i)));
        }

        //initialize array of pixel colours
        double[] pixels = new double[3 * res_x * res_y];
        int k = 0;
        for (int j = res_y - 1; j >= 0; j--) {
            for (int i = 0; i < res_x; i++) {
                int c = i; 
                int r = j;
                //compute pixel location in camera coordinates given formula in slides
                uc = -right + right * ((2*(float)c) / (float)res_x);
                vr = -top + top * ((2*(float)r) / (float)res_y);

                //construct ray from eye(0,0,0) to the pixel 
                Ray cur_ray = new Ray();

                cur_ray.start_point = new Matrix(new double[]{0, 0, 0}, 3);
                cur_ray.direction = new Matrix(new double[]{uc, vr, -near}, 3);

                cur_ray.direction = cur_ray.direction.times(1.0/cur_ray.direction.norm2()); //normalize
                cur_ray.setDepth(1);

                //call raytrace to determine colour at pixel
                Matrix colour = raytrace(cur_ray);
                //assign result to pixels array
                pixels[k] = Math.min(255, 255*colour.get(0, 0));
                pixels[k + 1] = Math.min(255, 255*colour.get(1,0));
                pixels[k + 2] = Math.min(255, 255*colour.get(2, 0));
                //increment k by 3
                k = k + 3;

            }
        }
       
        //output the image in PPM P3 format
        outputImage(pixels, k); 
    }
}
    