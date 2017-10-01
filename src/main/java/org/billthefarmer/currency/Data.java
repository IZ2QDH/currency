////////////////////////////////////////////////////////////////////////////////
//
//  Currency - An android currency converter.
//
//  Copyright (C) 2016	Bill Farmer
//
//  This program is free software: you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation, either version 3 of the License, or
//  (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License
//  along with this program.  If not, see <http://www.gnu.org/licenses/>.
//
//  Bill Farmer	 william j farmer [at] yahoo [dot] co [dot] uk.
//
///////////////////////////////////////////////////////////////////////////////

package org.billthefarmer.currency;

import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.net.URL;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Data class
public class Data
{
    private static Data instance;

    private Map<String, Double> map;
    private List<Integer> list;

    private TaskCallbacks callbacks;
    private boolean parsing;

    // Constructor
    private Data()
    {
    }

    // Get instance
    public static Data getInstance(TaskCallbacks callbacks)
    {
        if (instance == null)
            instance = new Data();

        instance.callbacks = callbacks;
        return instance;
    }

    // Set list
    public void setList(List<Integer> list)
    {
        this.list = list;
    }

    // Get list
    public List<Integer> getList()
    {
        return list;
    }

    // Set map
    public void setMap(Map<String, Double> map)
    {
        this.map = map;
    }

    // Get map
    public Map<String, Double> getMap()
    {
        return map;
    }

    // Start list task
    protected void startListTask(String url)
    {
        ListTask listTask = new ListTask();
        listTask.execute(url);
    }

    // ListTask class
    protected class ListTask
        extends AsyncTask<String, String, Map<String, Double>>
    {
        // The system calls this to perform work in a worker thread
        // and delivers it the parameters given to AsyncTask.execute()
        @Override
        protected Map<String, Double> doInBackground(String... urls)
        {
            Map<String, Double> map = new HashMap<String, Double>();
            InputStream stream = null;
            BufferedReader reader = null; 
            String date = null;

            try
            {
                URL url = new URL(urls[0]);
                stream = url.openStream();
                reader = new BufferedReader(new InputStreamReader(stream));
                String line;
                do
                {
                    line = reader.readLine();
                    if (line.matches("[0-9]2-[a-zA-Z]3-[0-9]2"))
                        date = line;

                    else
                    {
                        String fields[] = line.split("\t");
                        if (fields.length == 3)
                        {
                            Double rate;

                            try
                            {
                                rate = Double.parseDouble(fields[2]);
                            }

                            catch (Exception e)
                            {
                                rate = 1.0;
                            }

                            if (fields[1] == "")
                            {
                                map.put(fields[0], rate);
                            }

                            else if (fields[1] == "1")
                            {
                                map.put(fields[0], rate / 1.0);
                            }
                        }
                    }
                } while (line != null);
            }

            catch (Exception e)
            {
                map.clear();
            }

            finally
            {
                if (reader != null)
                {
                    try
                    {
                        reader.close();
                    }

                    catch (Exception e) {}
                }

                else if (stream != null)
                {
                    try
                    {
                        stream.close();
                    }

                    catch (Exception e) {}
                }
            }

            publishProgress(date);
            return map;
        }

        // On progress update
        @Override
        protected void onProgressUpdate(String... date)
        {
            if (callbacks != null)
                callbacks.onProgressUpdate(date);
        }

        // The system calls this to perform work in the UI thread and
        // delivers the result from doInBackground()
        @Override
        protected void onPostExecute(Map<String, Double> map)
        {
            if (callbacks != null)
                callbacks.onPostExecute(map);
        }
    }

    // Start parse task
    protected void startParseTask(String url)
    {
        ParseTask parseTask = new ParseTask();
        parseTask.execute(url);
    }

    // ParseTask class
    protected class ParseTask
        extends AsyncTask<String, String, Map<String, Double>>
    {
        String latest;

        // The system calls this to perform work in a worker thread
        // and delivers it the parameters given to AsyncTask.execute()
        @Override
        protected Map<String, Double> doInBackground(String... urls)
        {
            // Get a parser
            Parser parser = new Parser();

            // Start the parser and report progress with the date
            if (parser.startParser(urls[0]) == true)
                publishProgress(parser.getDate());

            // Return the map
            return parser.getMap();
        }

        // On progress update
        @Override
        protected void onProgressUpdate(String... date)
        {
            if (callbacks != null)
                callbacks.onProgressUpdate(date);
        }

        // The system calls this to perform work in the UI thread and
        // delivers the result from doInBackground()
        @Override
        protected void onPostExecute(Map<String, Double> map)
        {
            if (callbacks != null)
                callbacks.onPostExecute(map);
        }
    }

    // TaskCallbacks interface
    interface TaskCallbacks
    {
        void onProgressUpdate(String... date);
        void onPostExecute(Map<String, Double> map);
    }
}
