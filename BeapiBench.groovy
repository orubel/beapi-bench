#!/usr/bin/env groovy

/*
 * Copyright 2013-2019 Beapi.io
 *
 * Licensed under the MPL-2.0 License;
 * you may not use this file except in compliance with the License.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@Grab(group='commons-cli', module='commons-cli', version='1.4')

import groovy.json.JsonSlurper
import java.text.DecimalFormat


class BeapiBench {


    static void main(String[] args) {
        CommandLineInterface cli = CommandLineInterface.INSTANCE
        cli.parse(args)
    }
}


/**
 * CLI Interface for tool
 */
enum CommandLineInterface{
    INSTANCE


    private List methods = ['GET', 'PUT', 'POST', 'DELETE']
    protected String method
    private List graphTypes = ['TIME','TOTALTIME','IO','ALL']
    protected String graphType = 'TIME'
    protected String endpoint
    protected Integer concurrency = 50
    protected Integer requests = 1000
    protected String token
    protected List headers
    protected String contentType = 'application/json'

    protected String path = '/tmp/'
    protected String filename = 'beapiBench.txt'
    protected String tmpPath = path+filename
    boolean noHardcore = true
    Float totalTime
    Integer testSize = 50
    Integer testTime = 60
    String postData

    CliBuilder cliBuilder

    CommandLineInterface(){
        cliBuilder = new CliBuilder(
            usage: 'BeapiBench.groovy [<options>] -m=method --endpoint=url',
            header: 'OPTIONS:',
            footer: "BeapiBench is a tool for benchmarking and graphing api's. It requires that both ApacheBench (ab) and gnuplot be preinstalled and available to run. Please make sure these are available and installed via your repository. If you have any questions, please visit us a http://beapi.io. Thanks again."
        )

        cliBuilder.width=80
        cliBuilder.with {
            // HELP OPT
            h(longOpt: 'help', 'Print this help text and exit (usage: -h, --help)')

            // FORCE OPT
            f(longOpt: 'force', 'Force run without checking for dependencies')

            // REQUIRED TEST OPTS
            m(longOpt:'method',args:2, valueSeparator:'=',argName:'property=value', 'request method for endpoint (GET/PUT/POST/DELETE)')
            _(longOpt:'endpoint',args:2, valueSeparator:'=',argName:'property=value', 'url for making the api call (usage: --endpoint=http://localhost:8080)')
            _(longOpt:'testnum',args:2, valueSeparator:'=',argName:'property=value', 'number of tests to run; defaults to 50 (usage: --testNum=100)')
            _(longOpt: 'hardcore', 'No pause between tests')
            g(longOpt:'graphtype',args:2, valueSeparator:'=',argName:'property=value', 'type of graph to create: TIME / TOTALTIME / ALL; defaults to TESTTIME (usage: -g TOTALTIME)')

            // OPTIONAL TEST OPTS
            c(longOpt:'concurrency',args:2, valueSeparator:'=',argName:'property=value', 'value for concurrent users per test run (usage: -c 50, --concurrency=50)')
            n(longOpt:'requests',args:2, valueSeparator:'=',argName:'property=value', 'requests to make per test run (usage: -n 1000, --requests=1000)')
            t(longOpt:'token',args:2, valueSeparator:'=',argName:'property=value', 'JWT bearer token (usage: -t wer4t56g356g356h35h, --token=wer4t56g356g356h35h)')
            H(longOpt:'header',args:2, valueSeparator:'=',argName:'property=value', 'optional header to pass (usage: -H <header>, --header=<header>)')
            j(longOpt:'contenttype',args:2, valueSeparator:'=',argName:'property=value', "content-type header; defaults to 'application/json' (usage: -c application/xml, --contenttype=application-xml)")
            p(longOpt:'postData',args:2, valueSeparator:'=',argName:'property=value', 'txt file supplying POST data (usage: -p post.txt )')

            // GRAPH OPTS

        }
    }

    void parse(args) {
        OptionAccessor options = cliBuilder.parse(args)
        try {

            if (!options.f) {
                /*
                // Apache-Utils
                String abChk = "dpkg -s ab &> /dev/null | echo \$?"
                def proc2 = ['bash', '-c', abChk].execute()
                proc2.waitFor()
                def outputStream2 = new StringBuffer()
                def error2 = new StringWriter()
                proc2.waitForProcessOutput(outputStream2, error2)
                String output2 = outputStream2.toString()
                switch (output2) {
                    case '0':
                        break;
                    case '1':
                    default:
                        throw new Exception('Apache-Utils not installed. Please install via your local repositorys or use -f (--force) to force run.')
                }

                String gnuChk = "dpkg -s gnuplot &> /dev/null | echo \$?"
                def proc3 = ['bash', '-c', gnuChk].execute()
                proc3.waitFor()
                def outputStream3 = new StringBuffer()
                def error3 = new StringWriter()
                proc3.waitForProcessOutput(outputStream3, error3)
                String output3 = outputStream3.toString()
                switch (output3) {
                    case '0':
                        break;
                    case '1':
                    default:
                        throw new Exception('Gnuplot not installed. Please install via your local repositorys or use --f (--force) to force run.')
                }
                */
            }

            if (!options) {
                throw new Exception('Could not parse command line options.\n')
            }
            if (options.h) {
                cliBuilder.usage()
                System.exit 0
            }

            if (options.m && options.endpoint) {
                if (!methods.contains(options.m.toUpperCase())) {
                    throw new Exception('Request method not supported. Please try again.\n')
                }
                this.method = options.m


                try {
                    URL url = new URL(options.endpoint)
                } catch (Exception e) {
                    throw new Exception('Endpoint is not a valid URL. Please try again', e)
                }
                this.endpoint = options.endpoint
            } else {
                throw new Exception('Method (--method) and Endpoint (--endpoint) are both REQUIRED for BeapiBench to work. Please try again.\n')
            }


            if (options.c) {
                try {
                    this.concurrency = options.c as Integer
                    if (!(this.concurrency>0)) {
                        throw new Exception('Concurrency must be a positive number greater than 0. Please try again.')
                    }
                } catch (Exception e) {
                    throw new Exception('Concurrency (--concurrency ,-c) expects an Unsigned Integer greater than 0. Please try again.', e)
                }
            }

            if (options.n) {
                try {
                    this.requests = options.n as Integer
                    if (!(this.requests>0)) {
                        throw new Exception('Number of requests must be a positive number greater than 0. Please try again.')
                    }
                } catch (Exception e) {
                    throw new Exception('Requests (--request, -n) expects an Unsigned Integer greater than 0. Please try again.', e)
                }
            }

            if (this.concurrency >= this.requests) {
                throw new Exception('Concurrency must be less than number of requests. Please try again')
            }

            if (options.t) {
                this.token = options.t.trim()
            }
            if (options.H) {
                options.H.each() {
                    this.headers.add(it.trim())

                }
            }
            if (options.p) {
                this.postData = options.p.trim()
            }
            if (options.j) {
                this.contentType = options.j.trim()
            }

            if (options.hardcore) {
                this.noHardcore = false
            }

            if (options.testnum) {
                try {
                    Integer temp = options.testnum as Integer
                    if (!(temp>0)) {
                        throw new Exception('Testnum (--testnum)must be a positive number greater than 0. Please try again.')
                    }
                    this.testSize=temp
                } catch (Exception e) {
                    throw new Exception('Testnum (--testnum) expects an Unsigned Integer greater than 0. Please try again.', e)
                }
            }

            if (options.g) {
                if (!this.graphTypes.contains(options.g)) {
                    throw new Exception("GraphType (--graphtype, -g) must match one of existing GraphTypes: [${this.graphTypes}]. Please try again.")
                }
                this.graphType = options.g
            }
        } catch (Exception e) {
            System.err << e
            System.exit 1
        }


        int i = 0
        List data = []
        while (i < this.testSize) {
            // call method; move this to method
            print("[TEST ${i+1} of ${this.testSize}] : ")
            List returnData = callApi(this.postData, this.concurrency, this.requests, this.contentType, this.token, this.method, this.endpoint, this.headers)
            if(!returnData.isEmpty()) {
                DecimalFormat df = new DecimalFormat("0.00")
                if (data.size() > 0) {
                    int size = data.size() - 1
                    // TODO: will fail here if tests fail; need to create a way to continue; test returnData
                    //try {
                        Float floatTemp1 = Float.parseFloat(returnData[1])
                        Float floatTemp2 = Float.parseFloat(data[size][1])
                        Float floatTemp3 = Float.sum(floatTemp1, floatTemp2)
                        List temp = [returnData[1], df.format(floatTemp3), returnData[2], df.format(Float.parseFloat(returnData[3])), df.format(Float.parseFloat(returnData[4])), df.format(Float.parseFloat(returnData[5])), df.format(Float.parseFloat(returnData[6])), df.format(Float.parseFloat(returnData[7])), df.format(Float.parseFloat(returnData[8])),returnData[9],returnData[10],returnData[11],returnData[12]]
                        data.add(temp)

                        this.totalTime = floatTemp3
                    //}catch(Exception e){
                    //    println("${returnData} :" +e)
                    //}
                } else {
                    // time/totaltime/rps
                    List temp = [returnData[1], returnData[1], returnData[2],returnData[3],returnData[4],returnData[5],returnData[6],returnData[7],returnData[8],returnData[9],returnData[10],returnData[11],returnData[12]]
                    data.add(temp)
                }
            }else{
                println(" TEST FAILED. Set concurrency/requests lower or change server config.")
            }

            if(this.noHardcore) {
                float waitTime = Float.parseFloat(returnData[1])
                waitTime = ((waitTime * 1000) * 2) + 250
                sleep(waitTime as Integer)
            }
            i++
        }


        // CREATE DATA FILE
        File apiBenchData = new File("${this.tmpPath}")
        if (apiBenchData.exists() && apiBenchData.canRead()) { apiBenchData.delete() }
        apiBenchData.append('# doc   sum   time   rps   success   fail   data   html   tpr   transferrate   connect   processing   waiting   ttime\n')

        i = 1
        data.each() {
            apiBenchData.append "${i}   "
            apiBenchData.append it.join('   ')
            apiBenchData.append '\n'
            i++
        }

        // CREATE GRAPH
        String title = "${this.concurrency} c / ${this.requests} n / ${this.testSize} tests}"
        if(this.graphType!='ALL') {
            createChart(this.graphType,"${title}")
        }else{
            this.graphTypes.each(){
                if(it!='ALL'){
                    createChart("${it}","${title}")
                }
            }
        }
    }

    // TODO
    protected testConnection(){

    }

    protected List callApi(String postData, Integer concurrency, Integer requests, String contentType, String token, String method, String endpoint, List headers) {
        String bench = "ab -c ${concurrency} -n ${requests}"
        if(postData){ bench += " -p ${this.postData}" }
        if(contentType){ bench +=  " -H 'Content-Type: ${contentType}'" }
        if(token){ bench +=  " -H 'Authorization: Bearer ${token}'" }
        if(headers){
            headers.each(){
                bench +=  " -H '${it}'"
            }
        }

        bench += " -m ${method} ${endpoint}"
        def proc = ['bash', '-c', bench].execute()
        proc.waitFor()
        DecimalFormat df = new DecimalFormat("0.00")
        def outputStream = new StringBuffer()
        def error = new StringWriter()
        proc.waitForProcessOutput(outputStream, error)
        String output = outputStream.toString()
        List<String> returnData = ['0', '0', '0', '0', '0', '0', '0', '0', '0']
        String finalOutput = ""
        if (output) {
            List lines = output.readLines()
            lines.each() { it2 ->
                if(it2.trim()) {
                    finalOutput += it2 + " "
                }
            }


                def group = (finalOutput =~ /Document Length:        ([0-9]+) bytes Concurrency Level:      ([0-9]+) Time taken for tests:   ([0-9\.]+) seconds Complete requests:      ([0-9]+) Failed requests:        ([0-9]+) Total transferred:      ([0-9]+) bytes HTML transferred:       ([0-9]+) bytes Requests per second:    ([0-9\.]+) \[#\\/sec\] \(mean\) Time per request:       ([0-9\.]+) \[ms\] \(mean\) Time per request:       ([0-9\.]+) \[ms\] \(mean, across all concurrent requests\) Transfer rate:          ([0-9\.]+) \[Kbytes\\/sec\] received Connection Times \(ms\)               min  mean\[\+\\/-sd\] median   max Connect: ( *[0-9]+) ( *[0-9]+) ( *[0-9\.]+) ( *[0-9]+) ( *[0-9]+) Processing: ( *[0-9]+) ( *[0-9]+) ( *[0-9\.]+) ( *[0-9]+) ( *[0-9]+) Waiting: ( *[0-9]+) ( *[0-9]+) ( *[0-9\.]+) ( *[0-9]+) ( *[0-9]+) Total: ( *[0-9]+) ( *[0-9]+) ( *[0-9\.]+) ( *[0-9]+) ( *[0-9]+)/)
                if (group.hasGroup() && group.size() > 0) {
                    println("[${group[0][1]}  bytes / ${group[0][11]} kb/s] : ${group[0][3]} secs/${group[0][8]} rps")
                    // Document Length
                    returnData[0] = df.format(Float.parseFloat(group[0][1].trim()))
                    // Time taken for tests
                    returnData[1] = df.format(Float.parseFloat(group[0][3]))
                    // Requests per second
                    returnData[2] = df.format(Float.parseFloat(group[0][8]))
                    // tests succeeded
                    returnData[3] = df.format(Float.parseFloat(group[0][4]))
                    // tests failed
                    returnData[4] = df.format(Float.parseFloat(group[0][5]))
                    // total data transferred (bytes)
                    returnData[5] = df.format(Float.parseFloat(group[0][6]))
                    // HTML transferred (bytes)
                    returnData[6] = df.format(Float.parseFloat(group[0][7]))
                    // time per request [ms]
                    returnData[7] = df.format(Float.parseFloat(group[0][10]))
                    // transfer rate
                    returnData[8] = df.format(Float.parseFloat(group[0][11]))


                    // Connect:      Most typically the network latency
                    //List connect = [Float.parseFloat(group[0][12].trim()), Float.parseFloat(group[0][13].trim()), Float.parseFloat(group[0][14].trim()), Float.parseFloat(group[0][15].trim()), Float.parseFloat(group[0][16].trim())]

                    returnData[9] = df.format(Float.parseFloat(group[0][13].trim()))
                    //println("### connect: ${returnData[9]}")

                    // Processing:   Time to receive full response after connection was opened
                    //List processing = [Float.parseFloat(group[0][17]), Float.parseFloat(group[0][18].trim()), Float.parseFloat(group[0][19].trim()), Float.parseFloat(group[0][20].trim()), Float.parseFloat(group[0][21].trim())]
                    returnData[10] = df.format(Float.parseFloat(group[0][19].trim()))
                    //println("### processing:"+returnData[10])

                    // Waiting:      Time-to-first-byte after the request was sent
                    //List waiting = [Float.parseFloat(group[0][22]), Float.parseFloat(group[0][23].trim()), Float.parseFloat(group[0][24].trim()), Float.parseFloat(group[0][25].trim()), Float.parseFloat(group[0][26].trim())]
                    returnData[11] = df.format(Float.parseFloat(group[0][24].trim()))
                    //println("### waiting:"+returnData[11])

                    // Total time
                    //List ttime = [Float.parseFloat(group[0][27]), Float.parseFloat(group[0][28].trim()), Float.parseFloat(group[0][29].trim()), Float.parseFloat(group[0][30].trim()), Float.parseFloat(group[0][31].trim())]
                    returnData[12] = df.format(Float.parseFloat(group[0][29].trim()))

                }else{
                    println("[REGEX ERROR: apiBench]:  Cannot match output")
                }
        } else {
            println("[ERROR: apiBench]:  Error message follows : " + error)
        }
        return returnData
    }


    protected void createChart(String graphType, String title){
        try{
            println("[tmp gnuplot file] >> "+this.tmpPath)

            String key = "set key left top;"
            String style = "set style textbox opaque;"
            String gridY = ""
            String gridX = ""
            String range
            String pointLabel
            String setTitle = ""
            String ylabel = ""
            String xlabel = ""
            String plot

            switch(graphType){
                case 'TIME':
                case 'TOTALTIME':
                    style = "set style textbox opaque;"
                    ylabel = "set ylabel \\\"RPS For Each Test\\\" ;"
                    switch(graphType){
                        case 'TIME':
                            //set output 'beapi_chart1.png'
                            xlabel = "set xlabel \\\"Seconds To Do ${this.requests} Requests\\\" ;"
                            setTitle = "set title \\\"Time For Each API Test  (Plot Points show time for each request in ms)\\\" ;"
                            gridY = "set grid ytics lc rgb \\\"#bbbbbb\\\" lw 1 lt 0;"
                            gridX = "set xrange [*:] reverse;set grid xtics lc rgb \\\"#bbbbbb\\\" lw 1 lt 0;"
                            // 3::0 sets every 3rd tic / 1:3:8 (x-range, y-range, string)
                            //pointLabel = "every 3::0 using 1:3:8 with labels center boxed notitle"
                            pointLabel = "every 3::0 using 2:4:9 with labels center boxed notitle"
                            //range = "1:3:1"
                            range = "2:4:2"
                            break
                        case 'TOTALTIME':
                            //set output 'beapi_chart2.png'
                            xlabel = "set xlabel \\\"Total Seconds For Tests\\\" ;"
                            setTitle = "set title \\\"Concatenated Time Of Concurrent API Tests (Plot Points show time for each request in ms)\\\" ;"
                            gridY = "set grid ytics lc rgb \\\"#bbbbbb\\\" lw 1 lt 0;"
                            gridX = "set grid xtics lc rgb \\\"#bbbbbb\\\" lw 1 lt 0;"
                            //pointLabel = "every 5::0 using 2:3:8 with labels center boxed notitle"
                            pointLabel = "every 5::0 using 3:4:9 with labels center boxed notitle"
                            //range = "2:3:2"
                            range = "3:4:3"
                            break
                    }
                    plot = "plot '${this.tmpPath}' using ${range} with linespoint pt 7 title \\\"${title}\\\",      ''          ${pointLabel}"
                    break
                case 'IO':
                    //set output 'beapi_chart3.png'
                    xlabel = "set xlabel \\\"Test # of ${this.testSize} Tests\\\" ;"
                    key = "set key right top;"
                    gridY = "set grid ytics lc rgb \\\"#bbbbbb\\\" lw 1 lt 0;"
                    style = "set style data histograms;set style histogram rowstacked gap 10; set style fill solid 0.5 border -1;"
                    ylabel = "set ylabel \\\"Total Time in Milliseconds\\\" ;"
                    gridX = "set xtics border in scale 0,0 nomirror center; set xrange [0:${this.testSize}] noreverse writeback;set x2range [ * : * ] noreverse writeback;"
                    plot = "plot '${this.tmpPath}' using 11 t \\\"connection time\\\", '' using 13 t \\\"waiting\\\", '' using 12:xtic(1) t \\\"processing\\\";"
                    break;
            }

            String bench = "gnuplot -p -e \"${gridX}${gridY}${xlabel}${ylabel}${setTitle}${key}${style}${plot};\""
            println(bench)
            def proc = ['bash', '-c', bench].execute()
            proc.waitFor()
            def outputStream = new StringBuffer()
            def error = new StringWriter()
            proc.waitForProcessOutput(outputStream, error)
        } catch (Exception e) {
            throw new Exception('Error creating chart. Exception follows:', e)
        }
    }
}













