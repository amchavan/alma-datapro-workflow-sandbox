from setuptools import setup

dependencies = ['adaptmb']

setup(name='adaptmb-demos-common',
      version='0.1',
      description='DRAWS Message Bus API',
      url='http://www.almaobservatory.org',
      author='ALMA',
      author_email='test@alma.cl',
      license='LGPL',
      packages=['alma', 'alma/adapt/examples/common'],
      install_requires=dependencies,
      zip_safe=False)
